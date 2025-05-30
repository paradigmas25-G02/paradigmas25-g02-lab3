package httpRequest;

public class RequestException extends Exception {
	public RequestException(int status, String url){
		super("status code: " + Integer.toString(status) + " url: " + url);
	}
}
