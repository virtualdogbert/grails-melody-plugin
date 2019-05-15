/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 * Global AST to add Validateable to command objects
 */

package grails.melody.plugin

import groovy.transform.CompileStatic
import net.bull.javamelody.MonitoringProxy
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.core.artefact.ServiceArtefactHandler

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class MelodyEnhancerASTTransformation extends AbstractASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        List<ClassNode> classes = sourceUnit.getAST().getClasses()
        ServiceArtefactHandler handler = new ServiceArtefactHandler()

        classes.each { ClassNode node ->

            if (handler.isArtefact(node)) {
                List<MethodNode> methods = node.methods

                for(MethodNode method: methods){
                    if(method.isPublic()){
                        BlockStatement methodBody = (BlockStatement) method.getCode()
                               List statements = methodBody.getStatements()
                               statements.add(0, enhanceService(node, method))
                    }
                }
            }
        }
    }

    private Statement enhanceService(ClassNode node, MethodNode method) {
        ClassNode monitoringProxy = new ClassNode(MonitoringProxy.class)

        Expression springCounter = new StaticMethodCallExpression(monitoringProxy, "getSpringCounter", ArgumentListExpression.EMPTY_ARGUMENTS)
        Expression isDisplayed = new MethodCallExpression(springCounter, "isDisplayed", ArgumentListExpression.EMPTY_ARGUMENTS)
        Expression bindContextIncludingCpu = new MethodCallExpression(springCounter, "bindContextIncludingCpu", new ConstantExpression("${node.name}.${method.name}".toString()))

        BooleanExpression booleanExpression = new BooleanExpression(isDisplayed)

        return new IfStatement(booleanExpression, new ExpressionStatement(bindContextIncludingCpu), new EmptyStatement())
    }
}
