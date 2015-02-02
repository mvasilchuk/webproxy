package com.mvas.webproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ArgumentParser {

    private static final Logger logger = LoggerFactory.getLogger(ArgumentParser.class);

    private static final HashMap<String, String> arguments = new HashMap<>();

    public static HashMap<String, String> getArguments(String[] args) throws IllegalArgumentException
    {
        return getArguments(args, null);
    }

    public static HashMap<String, String> getArguments(String[] args, String[] names) throws IllegalArgumentException
    {

        if(arguments.size() == 0 && args.length > 0)
        {
            synchronized(arguments)
            {
                for(String arg: args)
                {
                    //logger.debug("argument: " + arg);
                    if(arg.length() > 1)
                    {
                        if(arg.charAt(0) == '-')
                        {
                            if(arg.charAt(1) == '-')
                            {
                                String argSubs = arg.substring(2);
                                if(argSubs.length() > 0)
                                {
                                    String[] data = argSubs.split("=");
                                    if(data.length == 2)
                                    {
                                        //logger.debug("Added arg: " + data[0] + " -> " + data[1]);
                                        arguments.put(data[0], data[1]);
                                    }
                                    else if(data.length == 1)
                                    {
                                        arguments.put(data[0], null);
                                    }
                                }
                                else
                                    throw new IllegalArgumentException("Argument " + arg + " has incorrect length");
                            }
                            else
                            {
                                String params = arg.substring(1);
                                for(int index = 0; index < params.length(); index++)
                                {
                                    //logger.debug("Added flag: " + String.valueOf(params.charAt(index)));
                                    arguments.put(String.valueOf(params.charAt(index)), "");
                                }
                            }
                        }
                        else
                            throw new IllegalArgumentException("Argument " + arg + " not recognized");
                    }
                    else
                        logger.warn("Undefined command line argument '" + arg + "'");
                }
            }
        }

        if(names == null)
            return arguments;

        HashMap<String, String> result = new HashMap<>();
        for (String name : names) {
            for (Map.Entry<String, String> entry : arguments.entrySet()) {
                if (entry.getKey().equals(name)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return result;

    }
}