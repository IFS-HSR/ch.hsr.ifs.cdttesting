package ch.hsr.ifs.cdttesting.cdttest;

import java.lang.reflect.Field;

import ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin.ITestPreferencesMixin;


/**
 * This class contains the default constants usable in the source file based tests.
 * 
 * @author tstauber
 *
 */
public final class CDTTestingConfigConstants {

   /**
    * The name of the config file for example in a rts file:
    * 
    * <pre>
    * {@code 
    * //! Sentence describing this test 
    * //@.config 
    * markerPositions=2
    * //@foo.h
    * ....
    * //@main.cpp
    * .... 
    * }
    * </pre>
    */
   public static final String CONFIG_FILE_NAME = ".config";

   /**
    * This tag can be used in the config to set the primary file. By default the first encountered file in the rts test will be the primary file.
    * <p>
    * In this example the default primary file would be {@code foo.h}, but it was stated, that {@code main.cpp} should be the default file:
    * 
    * <pre>
    * {@code 
    * //! Sentence describing this test 
    * //@.config 
    * primaryFile=main.cpp
    * //@foo.h
    * ....
    * //@main.cpp
    * .... 
    * }
    * </pre>
    * 
    */
   public static final String PRIMARY_FILE = "primaryFile";

   /**
    * This tag is used for evaluated preferences. This means the {@link Field}s in {@link ITestPreferencesMixin#getPreferenceConstants()} will be used
    * to extract the preference identifiers. The values must occur in tuples. The test in this example would look up the value of the Field
    * {@code P_SHORT_MAPPING} and would open the preference with this identifier and set it to the value in the field {@code Intern_V_WIDTH_8}
    * 
    * <pre>
    * {@code 
    * //! Sentence describing this test 
    * //@.config 
    * setPreferenceEval=(P_SHORT_MAPPING|Intern_V_WIDTH_8)
    * //@foo.h
    * ....
    * //@main.cpp
    * .... 
    * }
    * </pre>
    */
   public static final String SET_PREFERENCES_EVAL = "setPreferencesEval";

   /**
    * This tag is used to set a preference to a value.
    * This example sets the preference with the id {@code ch.hsr.ifs.examplator.fancynesssize} to the value {@code 18}.
    * 
    * <pre>
    * {@code 
    * //! Sentence describing this test 
    * //@.config 
    * setPreference=(ch.hsr.ifs.examplator.fancynesssize|18)
    * //@foo.h
    * ....
    * //@main.cpp
    * .... 
    * }
    * </pre>
    */
   public static final String SET_PREFERENCES = "setPreferences";

   /**
    * This tag is used to tell a {@link CDTTestingCheckerTest} on which line it should expect a marker.
    * 
    * <pre>
    * {@code 
    * //! Sentence describing this test 
    * //@.config 
    * markerLines=2,4,5
    * //@foo.h
    * ...
    * - Line which would trigger a checker -
    * ...
    * - Line which would trigger a checker -
    * ...
    * - Line which would trigger a checker -
    * ....
    * //@main.cpp
    * .... 
    * }
    * </pre>
    * 
    * This tag is mutually exclusive with {@link #SET_PREFERENCES_EVAL}
    */
   public static final String MARKER_LINES = "markerLines";

   private CDTTestingConfigConstants() {}

}
