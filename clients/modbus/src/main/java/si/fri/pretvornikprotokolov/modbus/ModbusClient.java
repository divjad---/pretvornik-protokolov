package si.fri.pretvornikprotokolov.modbus;

import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
public class ModbusClient extends AbstractModbusRequestHandler {

    public ModbusClient(ModbusMaster master) {
        super(master);
    }

    @Override
    public String processStreamRequest(String fromTopic, byte[] data) {
        return null;
    }

    @Override
    public String processReplyRequest(String fromTopic, byte[] data) {
        return null;
    }
}
