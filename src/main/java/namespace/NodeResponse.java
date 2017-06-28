package namespace;

/**
 * Responses for functions operating on the Nodes in the service
 * 
 * @author Wm. Keith van der Meulen
 *
 * @param <E> Type of object to be wrapped in the Response object
 */
class NodeResponse<E> extends Response<E> {
	
	/**
	 * Constructor for NodeResponse
	 * 
	 * @param value The object to be returned
	 * @param response NodeResponseCode detailing success/failure
	 */
	NodeResponse(E value, NodeResponseCode response) {
		super(value, response);
	}
	
	/**
	 * Response codes specific to functions operation on Nodes
	 * 
	 * @author Wm. Keith van der Meulen
	 */
	static enum NodeResponseCode implements ResponseCode {
		
		SUCCESS("The operation was successful."),
		ERROR_INTERNAL("An internal problem with the service occured."),
		ERROR_OTHER("An unidentified error occured."),
		NULL("Invalid return.");
		
		/**
		 * Message detailing success/failure of operation
		 */
		private String message;
		
		/**
		 * Constructor for ResponseCode
		 * 
		 * @param message Message detailing success/failure of operation
		 */
		NodeResponseCode(String message) {
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
}
