package at.rovo.parser;

public class InvalidAncestorException extends Exception
{
	/** **/
	private static final long serialVersionUID = -2115791396790462161L;

	public InvalidAncestorException(String msg)
	{
		super(msg);
	}
	
	public InvalidAncestorException(String msg, Throwable t)
	{
		super(msg, t);
	}
}
