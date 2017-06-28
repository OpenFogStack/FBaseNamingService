package namespace;

/**
 * Objects extending the Response class contain an object and a Response Code
 * allowing a method to return an object with information in the Response Code
 * detailing the success or failure of the call.
 * 
 * @author Wm. Keith van der Meulen
 *
 * @param <E> Type of object to be wrapped in the Response object
 */
abstract class Response<E> {
	
	/**
	 * The object wrapped in the Response class
	 */
	private E value;
	
	/**
	 * ResponseCode detailing the success/failure of operation
	 */
	private ResponseCode response;

	/**
	 * Constructor for Response
	 * 
	 * @param value The object to be returned
	 * @param response ResponseCode detailing success/failure
	 */
	Response(E value, ResponseCode response) {
		this.value = value;
		this.response = response;
	}
	
	/**
	 * Returns the object wrapped in the Response class
	 * 
	 * @return The object that is returned in the Response
	 */
	E getValue() {
		return this.value;
	}
	
	/**
	 * Returns the message detailing the success/failure of operation
	 * 
	 * @return Message detailing the success/failure of operation
	 */
	String getMessage() {
		return this.response.getMessage();
	}
}
