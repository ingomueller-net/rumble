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

package org.rumbledb.items;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.rumbledb.api.Item;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.context.Name;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.FunctionsNonSerializableException;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.FunctionIdentifier;
import org.rumbledb.runtime.functions.base.FunctionSignature;
import org.rumbledb.types.ItemType;
import org.rumbledb.types.SequenceType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionItem extends Item {

    private static final long serialVersionUID = 1L;
    private FunctionIdentifier identifier;
    private List<Name> parameterNames;

    // signature contains type information for all parameters and the return value
    private FunctionSignature signature;
    private RuntimeIterator bodyIterator;
    private Map<Name, List<Item>> localVariablesInClosure;
    private Map<Name, JavaRDD<Item>> RDDVariablesInClosure;
    private Map<Name, Dataset<Row>> dataFrameVariablesInClosure;

    protected FunctionItem() {
        super();
    }

    public FunctionItem(
            FunctionIdentifier identifier,
            List<Name> parameterNames,
            FunctionSignature signature,
            RuntimeIterator bodyIterator
    ) {
        this.identifier = identifier;
        this.parameterNames = parameterNames;
        this.signature = signature;
        this.bodyIterator = bodyIterator;
        this.localVariablesInClosure = new HashMap<>();
        this.RDDVariablesInClosure = new HashMap<>();
        this.dataFrameVariablesInClosure = new HashMap<>();
    }

    public FunctionItem(
            FunctionIdentifier identifier,
            List<Name> parameterNames,
            FunctionSignature signature,
            RuntimeIterator bodyIterator,
            Map<Name, List<Item>> localVariablesInClosure,
            Map<Name, JavaRDD<Item>> RDDVariablesInClosure,
            Map<Name, Dataset<Row>> DFVariablesInClosure
    ) {
        this.identifier = identifier;
        this.parameterNames = parameterNames;
        this.signature = signature;
        this.bodyIterator = bodyIterator;
        this.localVariablesInClosure = localVariablesInClosure;
        this.RDDVariablesInClosure = RDDVariablesInClosure;
        this.dataFrameVariablesInClosure = DFVariablesInClosure;
    }

    public FunctionItem(
            Name name,
            Map<Name, SequenceType> paramNameToSequenceTypes,
            SequenceType returnType,
            RuntimeIterator bodyIterator
    ) {
        List<Name> paramNames = new ArrayList<>();
        List<SequenceType> parameters = new ArrayList<>();
        for (Map.Entry<Name, SequenceType> paramEntry : paramNameToSequenceTypes.entrySet()) {
            paramNames.add(paramEntry.getKey());
            parameters.add(paramEntry.getValue());
        }

        this.identifier = new FunctionIdentifier(name, paramNames.size());
        this.parameterNames = paramNames;
        this.signature = new FunctionSignature(parameters, returnType);
        this.bodyIterator = bodyIterator;
        this.localVariablesInClosure = new HashMap<>();
        this.RDDVariablesInClosure = new HashMap<>();
        this.dataFrameVariablesInClosure = new HashMap<>();
    }

    @Override
    public FunctionIdentifier getIdentifier() {
        return this.identifier;
    }

    @Override
    public List<Name> getParameterNames() {
        return this.parameterNames;
    }

    @Override
    public FunctionSignature getSignature() {
        return this.signature;
    }

    public RuntimeIterator getBodyIterator() {
        return this.bodyIterator;
    }

    public Map<Name, List<Item>> getLocalVariablesInClosure() {
        return this.localVariablesInClosure;
    }

    public Map<Name, JavaRDD<Item>> getRDDVariablesInClosure() {
        return this.RDDVariablesInClosure;
    }

    public Map<Name, Dataset<Row>> getDFVariablesInClosure() {
        return this.dataFrameVariablesInClosure;
    }

    @Override
    public boolean equals(Object other) {
        // functions can not be compared
        return false;
    }

    @Override
    public boolean getEffectiveBooleanValue() {
        return false;
    }

    @Override
    public boolean isAtomic() {
        return false;
    }

    @Override
    public boolean isTypeOf(ItemType type) {
        return type.equals(ItemType.functionItem) || type.equals(ItemType.item);
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public String serialize() {
        throw new FunctionsNonSerializableException();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Function\n");
        sb.append("Identifier:" + this.identifier + "\n");
        sb.append("Parameters: ");
        for (Name param : this.parameterNames) {
            sb.append(param + " ");
        }
        sb.append("Signature: " + this.signature + "\n");
        sb.append("Body:\n" + this.bodyIterator + "\n");
        sb.append("Closure:\n");
        sb.append("  Local:\n");
        for (Name name : this.localVariablesInClosure.keySet()) {
            sb.append("    " + name + " (" + this.localVariablesInClosure.get(name).size() + " items)\n");
            if (this.localVariablesInClosure.get(name).size() == 1) {
                sb.append("      " + this.localVariablesInClosure.get(name).get(0).serialize() + "\n");
            }
        }
        sb.append("  RDD:\n");
        for (Name name : this.RDDVariablesInClosure.keySet()) {
            sb.append("    " + name + " (" + this.RDDVariablesInClosure.get(name).count() + " items)\n");
        }
        sb.append("  Data Frames:\n");
        for (Name name : this.dataFrameVariablesInClosure.keySet()) {
            sb.append("    " + name + " (" + this.dataFrameVariablesInClosure.get(name).count() + " items)\n");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode()
            + String.join("", this.parameterNames.toString()).hashCode()
            + this.signature.hashCode();
    }

    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObject(output, this.identifier);
        kryo.writeObject(output, this.parameterNames);
        kryo.writeObject(output, this.signature.getParameterTypes());
        kryo.writeObject(output, this.signature.getReturnType());
        // kryo.writeObject(output, this.bodyIterator);
        kryo.writeObject(output, this.localVariablesInClosure);
        kryo.writeObject(output, this.RDDVariablesInClosure);
        kryo.writeObject(output, this.dataFrameVariablesInClosure);

        // convert RuntimeIterator to byte[] data
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this.bodyIterator);
            oos.flush();
            byte[] data = bos.toByteArray();
            output.writeInt(data.length);
            output.writeBytes(data);
        } catch (Exception e) {
            throw new OurBadException(
                    "Error converting functionItem-bodyRuntimeIterator to byte[]:" + e.getMessage()
            );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Kryo kryo, Input input) {
        this.identifier = kryo.readObject(input, FunctionIdentifier.class);
        this.parameterNames = kryo.readObject(input, ArrayList.class);
        List<SequenceType> parameters = kryo.readObject(input, ArrayList.class);
        SequenceType returnType = kryo.readObject(input, SequenceType.class);
        this.signature = new FunctionSignature(parameters, returnType);
        // this.bodyIterator = kryo.readObject(input, RuntimeIterator.class);
        this.localVariablesInClosure = kryo.readObject(input, HashMap.class);
        this.RDDVariablesInClosure = kryo.readObject(input, HashMap.class);
        this.dataFrameVariablesInClosure = kryo.readObject(input, HashMap.class);

        try {
            int dataLength = input.readInt();
            byte[] data = input.readBytes(dataLength);
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            this.bodyIterator = (RuntimeIterator) ois.readObject();
        } catch (Exception e) {
            throw new OurBadException(
                    "Error converting functionItem-bodyRuntimeIterator to functionItem:" + e.getMessage()
            );
        }
    }

    @Override
    public ItemType getDynamicType() {
        return ItemType.functionItem;
    }

    public FunctionItem deepCopy() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            byte[] data = bos.toByteArray();
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (FunctionItem) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new OurBadException("Error while deep copying the function body runtimeIterator");
        }
    }

    public void populateClosureFromDynamicContext(DynamicContext dynamicContext, ExceptionMetadata metadata) {
        for (Name variable : dynamicContext.getLocalVariableNames()) {
            this.localVariablesInClosure.put(variable, dynamicContext.getLocalVariableValue(variable, metadata));
        }
        for (Name variable : dynamicContext.getRDDVariableNames()) {
            this.RDDVariablesInClosure.put(variable, dynamicContext.getRDDVariableValue(variable, metadata));
        }
        for (Name variable : dynamicContext.getDataFrameVariableNames()) {
            this.dataFrameVariablesInClosure.put(
                variable,
                dynamicContext.getDataFrameVariableValue(variable, metadata)
            );
        }
    }
}
