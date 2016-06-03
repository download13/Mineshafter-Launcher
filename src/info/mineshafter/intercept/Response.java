package info.mineshafter.intercept;

public class Response extends Message {
	private int status;

	public Response(byte[] b) {
		status = 200;
		body = b;
	}

	public Response(String b) {
		status = 200;
		body = b.getBytes();
	}

	public Response(int stat, String b) {
		status = stat;
		body = b.getBytes();
	}

	public Response(int stat, byte[] b) {
		status = stat;
		body = b;
	}

	public int getStatus() {
		return status;
	}
}
