import java.util.*;

public class STC extends Object
{
    String typeQualifier;
    String type;
    String name;

    boolean preDeclared = false;
    boolean writtenTo = false;
    boolean readFrom = false;

    public STC(String typeQualifier, String name, boolean preDeclared)
    {
        this.typeQualifier = typeQualifier;
        this.name = name;
        this.preDeclared = preDeclared;
    }

    public STC(String typeQualifier, String name, String type, boolean preDeclared)
    {
        this.typeQualifier = typeQualifier;
        this.name = name;
        this.type = type;
        this.preDeclared = preDeclared;
    }

    public void addType(String type)
    {
        this.type = type;
    }

    public void write()
    {
        this.writtenTo = true;
    }

    public void read()
    {
        this.readFrom = true;
    }
}
