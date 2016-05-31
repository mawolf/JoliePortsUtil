package jolie.ports.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.Interpreter;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.net.ports.Interface;
import jolie.net.ports.OutputPort;
import jolie.process.NullProcess;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;

/**
 *
 * @author mawo
 */
public class PortsUtils extends JavaService {

    private final Interpreter interpreter;

    public PortsUtils() {
        this.interpreter = Interpreter.getInstance();
    }

    private String[] getArgs(String filename) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String s : interpreter().includePaths()) {
            builder.append(s);
            if (++i < interpreter().includePaths().length) {
                builder.append(jolie.lang.Constants.pathSeparator);
            }
        }

        return new String[]{
            "-i",
            builder.toString(),
            filename
        };
    }

    @RequestResponse
    public Value createOutputFromInput(Value request) {
        String[] args = getArgs(request.getFirstChild("filename").strValue());
        CommandLineParser cmdParser = null;
        try {
            cmdParser = new CommandLineParser(args, PortsUtils.class.getClassLoader());
        } catch (CommandLineException | IOException ex) {
            ex.printStackTrace();
        }
        Program program = null;
        try {
            program = ParsingUtils.parseProgram(
                    cmdParser.programStream(),
                    cmdParser.programFilepath().toURI(), cmdParser.charset(),
                    cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ProgramInspector inspector = ParsingUtils.createInspector(program);
        Map< String, OneWayTypeDescription> oneWayMap = new HashMap<>();
        Map< String, RequestResponseTypeDescription> rrMap = new HashMap<>();
        for (InputPortInfo inputPort : inspector.getInputPorts()) {
            for (InterfaceDefinition interfaceDefinition : inputPort.getInterfaceList()) {
                for (OperationDeclaration operationDeclaration : interfaceDefinition.operationsMap().values()) {
                    if (operationDeclaration instanceof OneWayOperationDeclaration) {
                        oneWayMap.put(operationDeclaration.id(), new OneWayTypeDescription(Type.UNDEFINED));
                    } else {
                        rrMap.put(operationDeclaration.id(), new RequestResponseTypeDescription(Type.UNDEFINED, Type.UNDEFINED, new HashMap<>()));
                    }
                }
            }
        }
        OutputPort outputPort = null;
        try {
            outputPort = new OutputPort(interpreter,
                    "UDSM",
                    null,
                    NullProcess.getInstance(),
                    new URI("local"),
                    new Interface(oneWayMap, rrMap), true);
        } catch (URISyntaxException ex) {
            Logger.getLogger(PortsUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        interpreter.register(outputPort.id(), outputPort);
        return Value.create(Boolean.TRUE);
    }

}
