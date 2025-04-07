package exceptions;

public class InstructionNameException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InstructionNameException(String msg) {
		super(msg);
	}
}
