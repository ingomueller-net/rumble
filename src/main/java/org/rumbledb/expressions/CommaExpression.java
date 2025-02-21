/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Irimescu, Can Berker Cikis
 *
 */

package org.rumbledb.expressions;


import org.rumbledb.compiler.VisitorConfig;
import org.rumbledb.exceptions.ExceptionMetadata;
import sparksoniq.jsoniq.ExecutionMode;

import java.util.ArrayList;
import java.util.List;

public class CommaExpression extends Expression {

    private final List<Expression> expressions;

    public CommaExpression(List<Expression> expressions, ExceptionMetadata metadata) {
        super(metadata);
        this.expressions = expressions;
    }

    // Empty sequence
    public CommaExpression(ExceptionMetadata metadata) {
        super(metadata);
        this.expressions = new ArrayList<>();
    }

    public List<Expression> getExpressions() {
        return this.expressions;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        if (this.expressions != null) {
            this.expressions.forEach(e -> {
                if (e != null) {
                    result.add(e);
                }
            });
        }
        return result;
    }

    @Override
    public void initHighestExecutionMode(VisitorConfig visitorConfig) {
        if (this.expressions.isEmpty()) {
            this.highestExecutionMode = ExecutionMode.LOCAL;
            return;
        }

        for (Expression expression : this.expressions) {
            if (!expression.getHighestExecutionMode(visitorConfig).isRDD()) {
                this.highestExecutionMode = ExecutionMode.LOCAL;
                return;
            }
        }

        this.highestExecutionMode = ExecutionMode.RDD;
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitCommaExpression(this, argument);
    }

}

