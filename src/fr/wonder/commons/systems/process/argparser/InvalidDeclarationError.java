package fr.wonder.commons.systems.process.argparser;

public class InvalidDeclarationError extends Error {

	private static final long serialVersionUID = -4865153623430088860L;

    public InvalidDeclarationError(String message) {
        super(message);
    }
    
    public InvalidDeclarationError(String message, Throwable cause) {
        super(message, cause);
    }

}
