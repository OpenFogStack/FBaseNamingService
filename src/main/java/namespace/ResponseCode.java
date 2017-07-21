package namespace;

/**
 * Interface for Enums that contain all the different response
 * codes from a function call
 * 
 * @author Wm. Keith van der Meulen
 */
public enum ResponseCode {
	
	SUCCESS("The operation was successful."),
	ERROR_ILLEGAL_COMMAND("This command is not allowed"),
	ERROR_INTERNAL("An internal problem with the service occured."),
	ERROR_INVALID_CONTENT("Unable to parse content to KeyGroup."),
	ERROR_ALREADY_EXISTS("Entity already exists."),
	ERROR_DOESNT_EXIST("Entity doesn't exist."),
	ERROR_IS_ACTIVE("Entity is active."),
	ERROR_TOMBSTONED("Entity is tombstoned."),
	ERROR_OTHER("An unidentified error occured."),
	NULL("Invalid return.");
	
	/**
	 * Message detailing success/failure of operation
	 */
	private String message;
	
	/**
	 * Constructor for enum
	 * 
	 * @param message Message detailing success/failure of operation
	 */
	ResponseCode(String message) {
		this.message = message;
	}
	
	/**
	 * Returns message detailing success/failure of operation
	 * 
	 * @return Message detailing the success/failure of operation
	 */
	public String getMessage() {
		return this.message;
	}
}