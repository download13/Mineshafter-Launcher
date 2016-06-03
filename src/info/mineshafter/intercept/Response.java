package info.mineshafter.intercept;

public class Response extends Message {
	public int status;

	public Response(byte[] b) {
		this(200, b);
	}
	
	public Response(int stat, String b) {
		status = stat;
		body = b.getBytes();
	}

	public Response(int stat, byte[] b) {
		status = stat;
		body = b;
	}
}
