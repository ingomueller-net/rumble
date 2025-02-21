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

package org.rumbledb.runtime.functions.sequences.general;

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.items.ItemFactory;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.LocalFunctionCallIterator;
import sparksoniq.jsoniq.ExecutionMode;

import java.util.List;

public class ExistsFunctionIterator extends LocalFunctionCallIterator {


    private static final long serialVersionUID = 1L;
    private RuntimeIterator sequenceIterator;

    public ExistsFunctionIterator(
            List<RuntimeIterator> parameters,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(parameters, executionMode, iteratorMetadata);
        this.sequenceIterator = this.children.get(0);
    }

    @Override
    public Item next() {
        if (this.hasNext()) {
            this.hasNext = false;
            if (this.sequenceIterator.isRDD()) {
                List<Item> i = this.sequenceIterator.getRDD(this.currentDynamicContextForLocalExecution).take(1);
                return ItemFactory.getInstance().createBooleanItem(!i.isEmpty());
            }
            this.sequenceIterator.open(this.currentDynamicContextForLocalExecution);
            Item result;
            if (this.sequenceIterator.hasNext()) {
                result = ItemFactory.getInstance().createBooleanItem(true);

            } else {
                result = ItemFactory.getInstance().createBooleanItem(false);
            }
            this.sequenceIterator.close();
            return result;
        }
        throw new IteratorFlowException(FLOW_EXCEPTION_MESSAGE + "exists function", getMetadata());
    }
}
