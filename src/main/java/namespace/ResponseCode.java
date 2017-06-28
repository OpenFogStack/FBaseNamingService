package namespace;

/**
 * Interface for Enums that contain all the different response
 * codes from a function call
 * 
 * @author Wm. Keith van der Meulen
 */
interface ResponseCode {
	
	/**
	 * Returns the message detailing the success/failure of operation
	 * 
	 * @return Message detailing the success/failure of operation
	 */
	String getMessage();

}
