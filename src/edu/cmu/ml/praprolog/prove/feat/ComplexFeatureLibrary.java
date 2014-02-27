package edu.cmu.ml.praprolog.prove.feat;

import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.util.Dictionary;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Malcolm Greaves
 */
public class ComplexFeatureLibrary {

    static public final String ESCAPE_PREFIX = "escape__";
    static protected Logger log = Logger.getLogger(ComplexFeatureLibrary.class);
    //
    static private boolean initialized = false;
    static private Map<String, ComplexFeature> functor2cf;
    static private LogicProgram program;

    static public boolean isComplexFeature(Goal g) {
        if (g == null)
            throw new IllegalArgumentException("goal cannot be null");
        return isComplexFeature(g.getFunctor());
    }

    static public boolean isComplexFeature(String functor) {
        if (functor == null)
            throw new IllegalArgumentException("functor cannot be null");
        return functor.startsWith(ESCAPE_PREFIX);
    }

    /**
     * @param functor
     * @return the ComplexFeature for the registered functor, null if functor is not registered
     * @throws IllegalArgumentException if not iniitalized
     * @throws NullPointerException     if functor is null
     * @throws IllegalArgumentException if functor is zero-length
     */
    static public ComplexFeature getFeature(String functor) {
        if (!initialized)
            throw new IllegalStateException("ComplexFeatureLibrary is not initialized");
        if (functor == null)
            throw new IllegalArgumentException("functor cannot be null");
        if (functor.length() == 0)
            throw new IllegalArgumentException("functor cannot be zero length");
        return functor2cf.get(functor);
    }

    static private void preInit(LogicProgram logicProgram) {
        if (logicProgram == null)
            throw new NullPointerException("logic program cannot be null");
        program = logicProgram;
        functor2cf = new HashMap<String, ComplexFeature>();
        if (initialized)
            log.warn("Complex feature library was already initialized once -- overwriting...");
    }

    static private void addEntry(String functor, String className, String[] args) {
        if (functor.isEmpty())
            throw new IllegalArgumentException("cannot have zero-length functor for feature " + className);

        final Class<? extends ComplexFeature> clazz;
        final ComplexFeature feature;
        try {
            clazz = (Class<? extends ComplexFeature>) Class.forName(className);
            feature = (ComplexFeature) clazz.getDeclaredConstructor(LogicProgram.class, String[].class)
                                            .newInstance(program, (Object) args);

            functor2cf.put(functor, feature);

            if (functor.startsWith(ESCAPE_PREFIX)) {
                String f = new String(functor.split(ESCAPE_PREFIX, 2)[1]);
                log.info("adding ComplexFeature: " + f);
                functor2cf.put(f, feature);
            } else {
                log.info("adding ComplexFeature: " + functor);
                functor2cf.put(ESCAPE_PREFIX + functor, feature);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Couldn't initialize feature " + className + " for functor " + functor +
                    " with args " + Dictionary.buildString(args, new StringBuilder(), ", "), e);
        }
    }

    /**
     * Destroys current state (if any) and loads the functor -> ComplexFeature mapping
     * from the properties file.
     *
     * @param logProg
     * @param f       - complex features properties file
     */
    static public void init(LogicProgram logProg, File f) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            init(logProg, r);
            r.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Couldn't find complex feature properties at " +
                                               f.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Trouble reading complex feature properties at " +
                                               f.getAbsolutePath(), e);
        }
    }

    /**
     * Destroys current state (if any) and loads the functor -> ComplexFeature mapping
     * from the specified reader.
     *
     * @param logProg
     * @param r       - reader for complex feature properties
     */
    static public void init(LogicProgram logProg, BufferedReader r) throws IOException {
        preInit(logProg);
        Properties props = new Properties();
        props.load(r);
        for (String functor : props.stringPropertyNames()) {
            String[] parts = props.getProperty(functor).split(",", 2);
            String clazz = parts[0];
            String[] args = parts.length > 1 ? parts[1].split(",") : new String[0];
            addEntry(functor, clazz, args);
        }
        initialized = true;
    }

    /**
     * Destroys current state (if any) and loads the functor -> ComplexFeature mapping
     * from the properties file.
     *
     * @param logProg
     * @param propsFile
     */
    static public void init(LogicProgram logProg, String propsFile) {
        if (propsFile == null)
            throw new NullPointerException("Complex feature properties file cannot be null");
        init(logProg, new File(propsFile));
    }

    /**
     * (Unit tests only)
     */
    static public void _reset() {
        initialized = false;
        functor2cf = null;
        program = null;
    }
}
