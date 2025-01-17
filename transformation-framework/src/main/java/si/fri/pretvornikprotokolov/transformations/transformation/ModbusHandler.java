package si.fri.pretvornikprotokolov.transformations.transformation;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.msg.ModbusRequestBuilder;
import com.intelligt.modbus.jlibmodbus.msg.base.ModbusRequest;
import com.intelligt.modbus.jlibmodbus.msg.base.ModbusResponse;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadCoilsResponse;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.utils.ModbusFunctionCode;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.transformations.configuration.models.MessageModel;
import si.fri.pretvornikprotokolov.transformations.configuration.models.ModbusModel;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
@ApplicationScoped
public class ModbusHandler {

    private ModbusHandler() {
        throw new IllegalStateException("Utility class");
    }

    protected static ModbusRequest buildModbusRequest(Map<Integer, Long> msgToRegisterMap, ModbusModel modbusModel, MessageModel messageModel) throws ModbusNumberException {
        ModbusRequest request = null;
        ModbusRequestBuilder requestBuilder = ModbusRequestBuilder.getInstance();
        int quantity = 8;

        while (!Modbus.checkEndAddress(modbusModel.getAddress() + quantity)) {
            quantity--;
        }

        switch (ModbusFunctionCode.get(messageModel.getFunctionCode())) {
            case READ_COILS -> request = requestBuilder.buildReadCoils(messageModel.getDeviceId(),
                    modbusModel.getAddress(),
                    quantity);
            case READ_DISCRETE_INPUTS -> request = requestBuilder.buildReadDiscreteInputs(messageModel.getDeviceId(),
                    modbusModel.getAddress(),
                    quantity);
            case READ_HOLDING_REGISTERS ->
                    request = requestBuilder.buildReadHoldingRegisters(messageModel.getDeviceId(),
                            modbusModel.getAddress(),
                            quantity);
            case READ_INPUT_REGISTERS -> request = requestBuilder.buildReadInputRegisters(messageModel.getDeviceId(),
                    modbusModel.getAddress(),
                    quantity);
            case WRITE_SINGLE_COIL -> {
                boolean value = msgToRegisterMap.containsKey(modbusModel.getAddress()) && msgToRegisterMap.get(modbusModel.getAddress()) == 1;
                request = requestBuilder.buildWriteSingleCoil(messageModel.getDeviceId(),
                        modbusModel.getAddress(),
                        value);
            }
            case WRITE_SINGLE_REGISTER -> request = requestBuilder.buildWriteSingleRegister(messageModel.getDeviceId(),
                    modbusModel.getAddress(),
                    Math.toIntExact(msgToRegisterMap.getOrDefault(modbusModel.getAddress(), 0L)));
            case READ_EXCEPTION_STATUS -> request = requestBuilder.buildReadExceptionStatus(messageModel.getDeviceId());
            case WRITE_MULTIPLE_COILS -> {
                boolean value = msgToRegisterMap.containsKey(modbusModel.getAddress()) && msgToRegisterMap.get(modbusModel.getAddress()) == 1;
                request = requestBuilder.buildWriteMultipleCoils(messageModel.getDeviceId(),
                        modbusModel.getAddress(),
                        new boolean[]{value});
            }
            case WRITE_MULTIPLE_REGISTERS ->
                    request = requestBuilder.buildWriteMultipleRegisters(messageModel.getDeviceId(),
                            modbusModel.getAddress(),
                            new int[]{Math.toIntExact(msgToRegisterMap.getOrDefault(modbusModel.getAddress(), 0L))});
            case READ_WRITE_MULTIPLE_REGISTERS ->
                    request = requestBuilder.buildReadWriteMultipleRegisters(messageModel.getDeviceId(),
                            modbusModel.getAddress(),
                            quantity,
                            modbusModel.getAddress(),
                            new int[]{Math.toIntExact(msgToRegisterMap.getOrDefault(modbusModel.getAddress(), 0L))});
            default -> log.warn("Function code not supported: {}", messageModel.getFunctionCode());
        }

        return request;
    }

    protected static void handleModbusResponse(ModbusResponse response, Map<Integer, Object> registerMap, ModbusModel modbusModel, MessageModel messageModel) throws IllegalDataAddressException {
        if (response.getFunction() != messageModel.getFunctionCode()) {
            log.warn("Function code mismatch! Response: {}, message model: {}", response.getFunction(), messageModel.getFunctionCode());
            return;
        }

        if (response.getModbusExceptionCode() != null) {
            log.warn("Modbus exception code: {}", response.getModbusExceptionCode());
        }

        switch (ModbusFunctionCode.get(messageModel.getFunctionCode())) {
            case READ_DISCRETE_INPUTS, READ_COILS -> {
                if (!modbusModel.getType().contains("bool")) {
                    throw new IllegalArgumentException("Wrong type");
                }

                ReadCoilsResponse coilsResponse = (ReadCoilsResponse) response;
                registerMap.put(modbusModel.getAddress(), coilsResponse.getModbusCoils().get(0));
            }
            case READ_WRITE_MULTIPLE_REGISTERS, READ_INPUT_REGISTERS, READ_HOLDING_REGISTERS -> {
                ReadHoldingRegistersResponse holdingRegistersResponse = (ReadHoldingRegistersResponse) response;

                getValueFromRegisters(holdingRegistersResponse, registerMap, modbusModel);
            }
            default -> log.warn("Function code not supported: {}", messageModel.getFunctionCode());
        }
    }

    protected static void getValueFromRegisters(ReadHoldingRegistersResponse response, Map<Integer, Object> registerMap, ModbusModel modbusModel) throws IllegalDataAddressException {
        if (modbusModel.getType().contains("int")) {
            if (modbusModel.getType().contains("8")) {
                registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getInt8At(0));
            } else if (modbusModel.getType().contains("16")) {
                registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getInt16At(0));
            } else if (modbusModel.getType().contains("64")) {
                registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getInt64At(0));
            } else {
                registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getInt32At(0));
            }
        } else if (modbusModel.getType().contains("long")) {
            registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getInt64At(0));
        } else if (modbusModel.getType().contains("float")) {
            if (modbusModel.getType().contains("64")) {
                registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getFloat64At(0));
            } else {
                registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getFloat32At(0));
            }
        } else if (modbusModel.getType().contains("double")) {
            registerMap.put(modbusModel.getAddress(), response.getHoldingRegisters().getFloat64At(0));
        } else {
            throw new IllegalArgumentException("Wrong type");
        }
    }
}
