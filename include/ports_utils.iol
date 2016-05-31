type CreateOutputFromInputReq:void {
	.filename:string
}
interface IPortsUtils {
RequestResponse:
	createOutputFromInput(CreateOutputFromInputReq)(bool)
}

outputPort PortsUtils {
Interfaces: IPortsUtils
}

embedded {
Java: "jolie.ports.util.PortsUtils" in PortsUtils
}
