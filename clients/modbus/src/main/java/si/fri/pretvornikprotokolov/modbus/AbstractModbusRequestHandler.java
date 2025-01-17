package si.fri.pretvornikprotokolov.modbus;

import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.msg.base.ModbusRequest;
import com.intelligt.modbus.jlibmodbus.msg.base.ModbusResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.AbstractRequestHandler;
import si.fri.pretvornikprotokolov.common.exceptions.HandlerException;
import si.fri.pretvornikprotokolov.common.interfaces.RequestHandler;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractModbusRequestHandler extends AbstractRequestHandler<ModbusMaster, ModbusResponse>
        implements RequestHandler<ModbusRequest, ModbusResponse> {

    @SneakyThrows
    protected AbstractModbusRequestHandler(ModbusMaster master) {
        this.client = master;
    }

    @Override
    public ModbusMaster getClient() {
        return this.client;
    }

    @Override
    public void requestReply(ModbusRequest request,
                             String device,
                             Callback<ModbusResponse> callback) throws HandlerException {
        try {
            if (!this.client.isConnected()) {
                this.client.connect();
            }

            callback.onNext(this.client.processRequest(request));
        } catch (Exception e) {
            throw new HandlerException("Error processing Modbus request", e);
        }
    }

    @Override
    public void disconnect() {
        if (this.client != null && this.client.isConnected()) {
            try {
                this.client.disconnect();
            } catch (ModbusIOException e) {
                log.error("Error disconnecting Modbus client", e);
            }
        }
    }
}
