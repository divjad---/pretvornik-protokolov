package si.fri.pretvornikprotokolov.modbus;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.intelligt.modbus.jlibmodbus.utils.FrameEvent;
import com.intelligt.modbus.jlibmodbus.utils.FrameEventListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ModbusTest {

    public static void main(String[] args) throws UnknownHostException, ModbusIOException, ModbusNumberException, ModbusProtocolException {
        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.out.println(record.getLevel().getName() + ": " + record.getMessage());
            }

            @Override
            public void flush() {
                //do nothing
            }

            @Override
            public void close() throws SecurityException {
                //do nothing
            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);

        TcpParameters tcpParameters = new TcpParameters();
        tcpParameters.setHost(InetAddress.getLocalHost());
        tcpParameters.setKeepAlive(true);
        tcpParameters.setPort(5022);
        ModbusMaster master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);
        master.addListener(new FrameEventListener() {
            @Override
            public void frameSentEvent(FrameEvent frameEvent) {
                System.out.println("Frame sent: " + frameEvent);
            }

            @Override
            public void frameReceivedEvent(FrameEvent frameEvent) {
                System.out.println("Frame received: " + frameEvent);
            }
        });

        // since 1.2.8
        if (!master.isConnected()) {
            master.connect();
        }

        System.out.println("Connected: " + master.isConnected());

        int[] registers = {32000, 32001, 32002, 32003, 32004, 32005, 32006};
        for (int register : registers) {
            try {
                ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest();
                request.setServerAddress(1);
                request.setStartAddress(register);
                request.setQuantity(10);
                request.setTransactionId(0);

                ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.processRequest(request);
                System.out.println("Response: " + response.getHoldingRegisters().get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
        HashMap<Integer, byte[]> map = new HashMap<>();

        for (int iii = 0; iii < 65000; iii++) {
            try {
                System.out.println("iii: " + iii);
                ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest();
                request.setServerAddress(1);
                request.setStartAddress(iii);
                request.setQuantity(10);
                request.setTransactionId(0);

                ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.processRequest(request);
                System.out.println("Response: " + Arrays.toString(response.getHoldingRegisters().getBytes()));

                map.put(iii, response.getHoldingRegisters().getBytes());
            } catch (Exception ignored) {

            }
        }

        System.out.println("Map size: " + map.size());
        // Map: {32000=[B@51972e05, 32001=[B@61f2cec0, 32002=[B@672fdc76, 32003=[B@7d32735a, 32004=[B@15bd8dff, 32005=[B@624ed546, 32006=[B@7b9b6915}
        System.out.println("Map: " + map);*/
    }
}
