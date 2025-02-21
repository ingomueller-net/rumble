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

package org.rumbledb.config;

import org.rumbledb.context.Name;
import org.rumbledb.exceptions.CliException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import sparksoniq.spark.SparkSessionManager;

import java.io.Serializable;
import java.util.HashMap;

public class RumbleRuntimeConfiguration implements Serializable, KryoSerializable {

    private static final long serialVersionUID = 1L;
    private static final String ARGUMENT_PREFIX = "--";
    private static final String ARGUMENT_FORMAT_ERROR_MESSAGE =
        "Invalid argument format. Required format: --property value";
    private HashMap<String, String> arguments;
    private static final RumbleRuntimeConfiguration defaultConfiguration = new RumbleRuntimeConfiguration();

    public RumbleRuntimeConfiguration() {
        this.arguments = new HashMap<>();
    }

    public RumbleRuntimeConfiguration(String[] args) {
        this.arguments = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (!args[i].startsWith(ARGUMENT_PREFIX)) {
                throw new CliException(ARGUMENT_FORMAT_ERROR_MESSAGE);
            }
            String argumentName = args[i].trim().replace(ARGUMENT_PREFIX, "");
            if (i + 1 >= args.length || args[i + 1].startsWith(ARGUMENT_PREFIX)) {
                throw new CliException("Missing argument value for a provided argument: " + argumentName + ".");
            }
            String argumentValue = args[i + 1];
            this.arguments.put(argumentName, argumentValue);
        }
    }

    public static RumbleRuntimeConfiguration getDefaultConfiguration() {
        return RumbleRuntimeConfiguration.defaultConfiguration;
    }

    public String getConfigurationArgument(String key) {
        if (this.arguments.containsKey(key)) {
            return this.arguments.get(key);
        } else {
            return null;
        }
    }

    public String getOutputPath() {
        if (this.arguments.containsKey("output-path")) {
            return this.arguments.get("output-path");
        } else {
            return null;
        }
    }

    public int getPort() {
        if (this.arguments.containsKey("port")) {
            return Integer.parseInt(this.arguments.get("port"));
        } else {
            return 8001;
        }
    }

    public boolean getOverwrite() {
        if (this.arguments.containsKey("overwrite")) {
            return this.arguments.get("overwrite").equals("yes");
        } else {
            return false;
        }
    }

    public boolean getShowErrorInfo() {
        if (this.arguments.containsKey("show-error-info")) {
            return this.arguments.get("show-error-info").equals("yes");
        } else {
            return false;
        }
    }

    public String getLogPath() {
        if (this.arguments.containsKey("log-path")) {
            return this.arguments.get("log-path");
        } else {
            return null;
        }
    }

    public String getQueryPath() {
        if (this.arguments.containsKey("query-path")) {
            return this.arguments.get("query-path");
        } else {
            return null;
        }
    }

    public int getResultSizeCap() {
        if (this.arguments.containsKey("result-size")) {
            return Integer.parseInt(this.arguments.get("result-size"));
        } else {
            return 200;
        }
    }

    public String getExternalVariableValue(Name name) {
        for (String s : this.arguments.keySet()) {
            if (s.equals("variable:" + name)) {
                return this.arguments.get(s);
            }
        }
        return null;
    }

    public boolean isShell() {
        if (this.arguments.containsKey("shell")) {
            return this.arguments.get("shell").equals("yes");
        } else {
            return false;
        }
    }

    public boolean isServer() {
        if (this.arguments.containsKey("server")) {
            return this.arguments.get("server").equals("yes");
        } else {
            return false;
        }
    }

    public boolean isPrintIteratorTree() {
        if (this.arguments.containsKey("print-iterator-tree")) {
            return this.arguments.get("print-iterator-tree").equals("yes");
        } else {
            return false;
        }
    }

    public boolean isLocal() {
        String masterConfig = SparkSessionManager.getInstance().getJavaSparkContext().getConf().get("spark.master");
        return masterConfig.contains("local");
    }

    @Override
    public String toString() {
        String result = "";
        result += "Master: "
            + SparkSessionManager.getInstance().getJavaSparkContext().getConf().get("spark.master")
            + "\n"
            +
            "Item Display Limit: "
            + getResultSizeCap()
            + "\n"
            +
            "Output Path: "
            + (this.arguments.getOrDefault("output-path", "-"))
            + "\n"
            +
            "Log Path: "
            + (this.arguments.getOrDefault("log-path", "-"))
            + "\n"
            +
            "Query Path : "
            + (this.arguments.getOrDefault("query-path", "-"))
            + "\n";
        return result;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObject(output, this.arguments);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Kryo kryo, Input input) {
        this.arguments = kryo.readObject(input, HashMap.class);
    }
}
