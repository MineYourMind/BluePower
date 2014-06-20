package net.quetzi.bluepower.tileentities.tier3;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Level;

import net.minecraft.tileentity.TileEntity;
import net.quetzi.bluepower.BluePower;
import net.quetzi.bluepower.tileentities.TileBase;

/**
 * @author fabricator77
 */
public class TileCPU extends TileBase implements IRedBusWindow {
	//config option
	public int cpuStoredCycles = 100000;
	public int cpuForTickCycles = 1; // default is 1000 for 20khz
	//front panel switches
	public byte deviceID = 0;
	public byte screenID = 1;
	public byte discDriveID = 2;
	public boolean halt = true;
	
	private byte[] memory;
	// registers
	private int programCounter;
	private int stackPointer;
	
	private int reg_X;
	private int reg_Y;
	private int reg_A; // accumulator
	private int reg_B; // 65EL02 ?
	private int reg_D; // ???
	private int reg_R; // 65EL02 ?
	// flags
	private boolean flag_C; // carry
	private boolean flag_D; // ?
	private boolean flag_E; // E flag 65C816
	private boolean flag_M; // M flag 65C816
	private boolean flag_N; // negative
	private boolean flag_O; // overflow
	private boolean flag_X; // ?
	private boolean flag_Z; // zero
	private boolean flag_BRK; // break flag
	private boolean flag_WAI = false;
	// redbus
	private int redbus_remote_address; // current remote redbus device
	private boolean redbus_timeout;
	private Object redbus_cache;
	
	private int availableCycles;
	
	// used in decoding opcodes
	private int effectiveAddress;
	private int BRKaddress;

	public TileCPU() {
		//TODO: make memory a config option
		this.memory = new byte[8192];
		powerOnReset();
	}
	
	public void powerOnReset () {
		//TODO: set start addresses
		this.programCounter = 1024;
		BRKaddress = 8192;
		stackPointer = 512;
		reg_R = 768;
		//front panel switches
		//TODO: read frontpanel from NBT data
		this.memory[0] = this.discDriveID;
		this.memory[1] = this.screenID;
		this.memory[2] = this.redbus_id; //redbus address
		// clear registers
		reg_A = reg_Y = reg_A = 0;
		//TODO: reset internal CPU flags
		flag_BRK = false;
		//load program/OS into memory
		String bootLoader = "/assets/bluepower/software/rpcboot.bin";
		InputStream disc = BluePower.class.getResourceAsStream(bootLoader);
		
		if (disc == null) {
			BluePower.log.info("[BluePowerControl] CPU failed to load bootloader "+bootLoader);
			return;
		}
		
		try {
			BluePower.log.info("[BluePowerControl] CPU loaded bootloader "+bootLoader);
			//loads 1K boot loader into memory
			disc.read(this.memory, 1024, 256);
			disc.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//TODO: NBT read/write
	
	public void updateEntity() {
		// 20 ticks per second = 20khz
		if (halt) return;
		
		this.flag_WAI = false;
		this.redbus_timeout = false;
		
		availableCycles += cpuForTickCycles;
		if (availableCycles > cpuStoredCycles) {
			availableCycles = cpuStoredCycles;
		}
		while (availableCycles > 0 && !this.flag_WAI && !this.redbus_timeout) {
			// executeInstruction();
			dumpMemory();
		}
	}
	
	private void dumpMemory () {
		if (this.programCounter > this.memory.length) {
			availableCycles = -1;
			halt = true;
			return;
		}
		availableCycles -= 1;
		String message = "[BluePowerControl] "+this.programCounter+": ";
		for (int i=0; i<16; i++) {
			message += Integer.toHexString(readMemory(this.programCounter) & 0xFF) + " ";
			this.programCounter++;
		}
		BluePower.log.info(message);
		this.availableCycles -= 1;
	}

	private void executeInstruction() {
		availableCycles -= 1;
		// TODO: read block of memory etc
		int opcode = readMemory(this.programCounter);
		BluePower.log.info("[BluePowerControl] CPU " + Integer.toHexString(this.programCounter) + ":"+Integer.toHexString(opcode));
		this.programCounter++;
		
		effectiveAddress = 0;
		//TODO: sort instruction according to frequency of usage
		// for speed reasons
		// Octal numbers for ease of lookup in documentation.
		switch (opcode) {
		// row 0
		case 0:// 6502 BRK (calls 65EL02 MMU #0)
			opBRK();
			break;
		case 0x01:// 6502 ORA (indirect,X)
			invalid(opcode);
			break;
		case 0x02:// 65E02 NXT
			invalid(opcode);
			break;
		case 0x03:// 65C816 ORA r,S
			invalid(opcode);
			break;
		case 0x04:// 65C02 TSB zeropage
			invalid(opcode);
			break;
		case 0x05:// 6502 ORA zeropage
			invalid(opcode);
			break;
		case 0x06:// 6502 ASL zeropage
			invalid(opcode);
			break;
		case 0x07:// 65EL02 ORA r,R
			invalid(opcode);
			break;	
		case 0x08:// 6502 PHP
			invalid(opcode);
			break;	
		case 0x09:// 6502 ORA #
			invalid(opcode);
			break;
		case 0x0A:// 6502 ASL A
			invalid(opcode);
			break;
		case 0x0B:// 65EL02 RHI
			invalid(opcode);
			break;
		case 0x0C:// 65C02 STB absolute
			invalid(opcode);
			break;
		case 0x0D:// 6502 ORA absolute
			invalid(opcode);
			break;
		case 0x0E:// 6502 ASL absolute
			invalid(opcode);
			break;
		case 0x0F:// 6502 MUL zeropage
			invalid(opcode);
			break;
			
		// row 1
		case 0x10:// 6502 BPL relative
			invalid(opcode);
			break;
		case 0x11:// 6502 ORA indirect, Y
			invalid(opcode);
			break;
		case 0x12:// 65C02 ORA indirect
			invalid(opcode);
			break;
		case 0x13:// 65C816 ORA (r,S),Y
			invalid(opcode);
			break;
		case 0x14:// 65C02 TRB zeropage
			invalid(opcode);
			break;
		case 0x15:// 6502 ORA zeropage,X
			invalid(opcode);
			break;
		case 0x16:// 6502 ASL zeropage,X
			invalid(opcode);
			break;
		case 0x17:// 65EL02 ORA (r,R),Y
			invalid(opcode);
			break;
		case 0x18:// 6502 CLC
			opCLC();
			break;
		case 0x19:// 6502 ORA absolute,Y
			invalid(opcode);
			break;
		case 0x1A:// 65C02 INC A
			invalid(opcode);
			break;
		case 0x1B:// 65EL02 RHX
			invalid(opcode);
			break;
		case 0x1C:// 65C02 TRB absolute
			invalid(opcode);
			break;
		case 0x1D:// 6502 ORA absolute,X
			invalid(opcode);
			break;
		case 0x1E:// 6502 ASL absolute,X
			invalid(opcode);
			break;
		case 0x1F:// 65EL02 MUL zeropage,X
			invalid(opcode);
			break;
			
		// row 2
		case 0x20:// 6502 JSR absolute
			invalid(opcode);
			break;
		case 0x21:// 6502 AND (indirect,X)
			invalid(opcode);
			break;
		case 0x22:// 65EL02 ENT
			invalid(opcode);
			break;
		case 0x23:// 65C816 AND r,S
			invalid(opcode);
			break;
		case 0x24:// 6502 BIT zeropage
			opBIT();
			break;
		case 0x25:// 6502 AND zeropage
			invalid(opcode);
			break;
		case 0x26:// 6502 ROL zeropage
			invalid(opcode);
			break;
		case 0x27:// 65EL02 AND r,R
			invalid(opcode);
			break;
		case 0x28:// 6502 PLP
			invalid(opcode);
			break;
		case 0x29:// 6502 AND #
			invalid(opcode);
			break;
		case 0x2A:// 6502 ROL A
			invalid(opcode);
			break;
		case 0x2B:// 65EL02 RLI
			invalid(opcode);
			break;	
		case 0x2C:// 6502 BIT absolute
			opBIT();
			break;
		case 0x2D:// 6502 AND absolute
			invalid(opcode);
			break;
		case 0x2E:// 6502 ROL absolute
			invalid(opcode);
			break;
		case 0x2F:// 65EL02 MUL absolute
			invalid(opcode);
			break;
			
		// row 3
		case 0x30:// 6502 BMI relative
			invalid(opcode);
			break;
		case 0x31:// 6502 
			invalid(opcode);
			break;
		case 0x32:// 6502 AND indirect, Y
			invalid(opcode);
			break;
		case 0x33:// 65C02 6502 AND indirect
			invalid(opcode);
			break;
		case 0x34:// 65C816 AND (r,S),Y
			invalid(opcode);
			break;
		case 0x35:// 65C02 BIT zeropage, X
			invalid(opcode);
			break;
		case 0x36:// 6502 ROL zeropage, X
			invalid(opcode);
			break;
		case 0x37:// 65EL02 AND (r,R),Y 
			invalid(opcode);
			break;
		case 0x38:// 6502 SEC
			invalid(opcode);
			break;
		case 0x39:// 6502 AND absolute, Y
			invalid(opcode);
			break;
		case 0x3A:// 65C02 DEC A
			invalid(opcode); 
			break;
		case 0x3B:// 65EL02 RLX
			invalid(opcode);
			break;
		case 0x3C:// 65C02 BIT absolute, X
			invalid(opcode);
			break;
		case 0x3D:// 6502 AND absolute, X
			invalid(opcode);
			break;
		case 0x3E:// 6502 ROL absolute, X
			invalid(opcode);
			break;
		case 0x3F:// 65EL02 MUL absolute, X
			invalid(opcode);
			break;
		
		// row 4
		case 0x40:// 6502 RTI
			invalid(opcode);
			break;
		
		// row 5
		case 0x50:// 6502 BVC relative
			invalid(opcode);
			break;
			
		// row 6
		case 0x60:// 6502 RTS
			invalid(opcode);
			break;
			
		// row 7
		case 0x70:// 6502 BVS relative
			invalid(opcode);
			break;
			
		// row 8
		case 0x80:// 65C02 BRA relative
			invalid(opcode);
			break;
			
		// row 9
		case 0x90:// 6502 BCC relative
			invalid(opcode);
			break;
			
		// row A
		case 0xA0:// 6502 LDY #
			invalid(opcode);
			break;
		case 0xA1:// 6502 LDA (indirect), X
			invalid(opcode);
			break;
		case 0xA2:// 6502 LDA #
			invalid(opcode);
			break;
		case 0xA3:// 65C816 LDA r, S
			invalid(opcode);
			break;
		case 0xA4:// 6502 LDY zeropage
			invalid(opcode);
			break;			
		case 0xA5:// 6502 LDA zeropage
			opLDA();
			break;
		case 0xA6:// 6502 LDY zeropage
			invalid(opcode);
			break;
		case 0xA7:// 65EL02 LDA r, R
			invalid(opcode);
			break;
		case 0xA8:// 6502 TAY
			opTAY();
			break;
		case 0xA9:// 6502 LDA #
			invalid(opcode);
			break;
		case 0xAA:// 6502 TAX
			opTAY();
			break;
		case 0xAB:// 65EL02 TRX
			invalid(opcode);
			break;
		case 0xAC:// 6502 LDY absolute
			invalid(opcode);
			break;
		case 0xAD:// 6502 LDA absolute
			invalid(opcode);
			break;
		case 0xAE:// 6502 LDX absolute
			invalid(opcode);
			break;
		case 0xAF:// 65EL02 TDA
			invalid(opcode);
			break;
			
		// row B
		case 0xB0:// 6502 BCS relative
			invalid(opcode);
			break;
			
		// row C
		case 0xC0:// 6502 CDY #
			invalid(opcode);
			break;
		case 0xC1:// 6502 CMP (indirect, X)
			invalid(opcode);
			break;
		case 0xC2:// 65C816 REP #
			opREP();
			break;
		case 0xC3:// 65C816 CMP r, S
			invalid(opcode);
			break;
		case 0xC4:// 6502 CPY zeropage
			invalid(opcode);
			break;
		case 0xC5:// 6502 CMP zeropage
			invalid(opcode);
			break;
		case 0xC6:// 6502 DEC zeropage
			invalid(opcode);
			break;
		case 0xC7:// 65EL02 CMP r,R
			invalid(opcode);
			break;
		case 0xC8:// 6502 INY
			invalid(opcode);
			break;
		case 0xC9:// 6502 CMP #
			invalid(opcode);
			break;
		case 0xCA:// 6502 DEX
			invalid(opcode);
			break;
		case 0xCB:// 65C02 WAI
			opWAI();
			break;
		case 0xCC:// 6502 CPY absolute
			invalid(opcode);
			break;
		case 0xCD:// 6502 CMP absolute
			invalid(opcode);
			break;
		case 0xCE:// 6502 DEC absolute
			invalid(opcode);
			break;
		case 0xCF:// 65EL02 PLD
			invalid(opcode);
			break;
			
		// row D
		case 0xD0:// 6502 BNE relative
			invalid(opcode);
			break;
		case 0xD1:// 6502 CMP (indirect), Y
			invalid(opcode);
			break;
		case 0xD2:// 65C02 CMP (indirect)
			invalid(opcode);
			break;
			
		// row E
		case 0xE0:// 6502 CPX #
			invalid(opcode);
			break;
		case 0xE1:// 6502 SBC (indirect, X)
			invalid(opcode);
			break;
		case 0xE2:// 65C816 SEP #
			invalid(opcode);
			break;
		case 0xE3:// 65C816 SBC r, S
			invalid(opcode);
			break;
		case 0xE4:// 6502 CPX zeropage
			invalid(opcode);
			break;
		case 0xE5:// 6502 SBC zeropage
			invalid(opcode);
			break;
		case 0xE6:// 6502 INC zeropage
			invalid(opcode);
			break;
		case 0xE7:// 65EL02 SBC r, R
			invalid(opcode);
			break;
		case 0xE8:// 6502 INX
			invalid(opcode);
			break;
		case 0xE9:// 6502 SBC #
			invalid(opcode);
			break;
		case 0xEA:// 6502 NOP
			// No OPerand
			// do nothing this cycle
			break;
		case 0xEB:// 65C816 XBA
			invalid(opcode);
			break;
		case 0xEC:// 6502 CPX absolute
			invalid(opcode);
			break;
		case 0xED:// 6502 SBC absolute
			invalid(opcode);
			break;
		case 0xEE:// 6502 INC absolute
			invalid(opcode);
			break;
		case 0xEF:// 65EL02 MMU
			opMMU1(readMemory(this.programCounter));
			break;
			
		// row F
		case 0xF0:// 6502 BEQ relative
			invalid(opcode);
			break;
		case 0xF1:// 6502 SBC (indirect),Y
			invalid(opcode);
			break;
		case 0xF2:// 65C02 SBC (indirect)
			invalid(opcode);
			break;
		case 0xF3:// 65C816 SBC (r,S),Y
			invalid(opcode);
			break;
		case 0xF4:// 65C816 PEA absolute
			invalid(opcode);
			break;
		case 0xF5:// 6502 SBC zeropage,X
			invalid(opcode);
			break;
		case 0xF6:// 6502 INC zeropage,X
			invalid(opcode);
			break;
		case 0xF7:// 65EL02 SBC (r,R),Y
			invalid(opcode);
			break;
		case 0xF8:// 6502 SED
			invalid(opcode);
			break;
		case 0xF9:// 6502 SBC absolute,Y
			invalid(opcode);
			break;
		case 0xFA:// 65C02 PLX
			invalid(opcode);
			break;
		case 0xFB:// 65C816 XCE relative
			opXCE();
			break;
		case 0xFC:// 65C816 JSR (absolute,X)
			invalid(opcode);
			break;
		case 0xFD:// 6502 SBC absolute,X
			invalid(opcode);
			break;
		case 0xFE:// 6502 INC absolute,X
			invalid(opcode);
			break;
		case 0xFF:// invalid
			invalid(opcode);
			break;
		}
		
	}
	
	// 65C816
	private void opREP() {
		// 65C816 Reset Processor Status Flag
		setFlags(getFlags() & (readMemory(this.programCounter) ^ 0xFFFFFFFF));
	}
	
	// 65EL02
	public void opMMU() {
		int t = this.reg_A & 0xFF;
	    if (t != this.redbus_remote_address) {
	    	// if output buffer full
	    	if (this.redbus_cache != null) {
	    		this.redbus_timeout = true;
	    	}
	        this.redbus_remote_address = t;
	    }
	}
	
	public void opMMU1(int mode) {
		this.programCounter++;
		
		switch (mode) {
			case 0:
				// set remote redbus address
				opMMU();
				break;
			case 1:
				// set redbus window to memory address in A
				break;
			case 2:
				// enable redbus window
				break;
			case 3:
				// set external memory mapped window to A.
				break;
			case 4:
				// enable external memory mapped window
				break;
			case 5:
				// set BRK address to A
				break;
			case 6:
				// set POR address to A
				break;
				
			case 0x80:
				// get remote redbus address
				this.reg_A = this.redbus_remote_address;
				break;
			case 0x81:
				// get redbus window to memory address in A
				break;
			case 0x82:
				// disable redbus window
				break;
			case 0x83:
				// get external memory mapped window to A.
				break;
			case 0x84:
				// disable external memory mapped window
				break;
			case 0x85:
				// get BRK address to A
				break;
			case 0x86:
				// get POR address to A
				break;
			case 0x87:
				// get RTC to A and D
				break;
			case 0xFF:
				// Output A register to MC logfile.
				break;
		}
	}
	
	private void opBRK() {
		// push program counter
		// push flags
		flag_BRK = true;
		programCounter = BRKaddress;
	}
	private void opXCE() {//65C816
		// exchange C and E flags
		boolean temp = flag_C;
		flag_E = flag_C;
		flag_C = temp;
	}
	
	private void opLDA() {
		this.reg_A = readMemory(effectiveAddress);
		this.programCounter++;
	}
	
	private void opWAI() {//65C02
		// wait for interrupt (actually waits for next world tick)
		this.flag_WAI = true;
	}

	// Modfied from sysmon
	private void opBIT() {
		// BIT opcode
		int tmp = readMemory(programCounter + effectiveAddress);
		flag_Z = ((reg_A & tmp) == 0);
		flag_N = ((tmp & 0x80) != 0);
		flag_O = ((tmp & 0x40) != 0);
	}
	private void opCLC() {
		// CLC opcode
		flag_C = false;
	}
	private void opTAX(){// 6502
		// TAX opcode
		// transfer A to X
		this.reg_X = this.reg_A;
	}
	private void opTAY(){// 6502
		// TAY opcode
		// transfer A to Y
		this.reg_Y = this.reg_A;
	}

	private int readMemory(int pc) {
		//TODO: redbus memory
		if (pc < memory.length) {//do not try to access more memory than we have.
			return (int)this.memory[pc] & 0xFF;
		}
		//TODO: throw error here
		return 0;
	}
	
	private void setFlags(int flag) {
		
	}
	
	private int getFlags() {
		return 0;
	}
	
	// Opcode functions
	private void invalid(int op) {
		throw new InvalidOPCodeException(Integer.toHexString(op));
	}
}
