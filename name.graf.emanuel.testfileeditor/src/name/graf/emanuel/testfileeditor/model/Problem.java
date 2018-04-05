package name.graf.emanuel.testfileeditor.model;

import org.eclipse.jface.text.Position;


public interface Problem {

   /**
    * Retrieve the line number the problem is occurring on
    * 
    * The line number is referenced to 1, as the Eclipse number ruler starts
    * counting at 1.
    * 
    * @return An integer greater or equal than 1.
    */
   int getLineNumber();

   /**
    * Get the position of the problem in the file
    * 
    * The position (consisting of offset and length) can be used to present
    * squiggly lines or other annotations in the editor for the given source
    * range.
    * 
    * @return The position of the problem in the file
    */
   Position getPosition();

   /**
    * Get the description of the problem
    * 
    * @return The description of the problem
    */
   String getDescription();

   int getSeverity();

   int getPrioriry();
}
