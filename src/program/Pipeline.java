package program;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import enums.HazardType;
import enums.Status;
import exceptions.LabelException;

public class Pipeline {

	// Cores para serem usadas no terminal
	public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[92m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CIAN = "\u001B[36m";
	
    // Tipo de tratamento de hazard
    private HazardType hazardType;
    // Status da predição de desvio
    private Status status;
    // Contador de programa
    public int pc;
    // Vetor de 5 estágios para o pipeline (IF, ID, EX, MEM, WB)
	private Instruction[] stages;
	// Número de ciclos
	private int cycle;
	// Variável auxiliar para tratamento da instrução MUL que executa em 2 ciclos
	private int tratamentoMul;
	// Vetor de memória com 40 posições
	public static int[] memory = new int[40];
	// Vetor de registradores com 32 posições
	public static int[] regs = new int[32];
	// Estrutura Map para mapear a posição do programa a ser desviada e alguma label
	public static Map<String, Integer> desvios = new HashMap<>();
	// Contador para número de bolhas adicionadas durante o Pipeline 
	private int stalls;
	// Contador para número de predições sucedidas
	private int truePredicts;
	
	public Pipeline() {
	}
	
	// Construtor do Pipeline
	public Pipeline(HazardType hazardType) {
		this.hazardType = hazardType;
		this.status = Status.STRONGLY_NOT_TAKEN;
		this.pc = 0;
        this.stages = new Instruction[5];
        this.tratamentoMul = 0;
        this.stalls = 0;
        this.truePredicts = 0;
        for (int i=0; i<5; i++) {
            stages[i] = new Instruction();
        }
        this.cycle = 0;
        
        // Inicialização dos valores na Memória
        for (int i=0; i<40; i++) {
        	memory[i] = 0;
        }
        
        // Inicialização dos valores nos Registradores
        for (int i=0; i<32; i++) {
        	regs[i] = 0;
        }
    }
	
	// Avança os estágios do Pipeline
	public void advance() {	
		// Verifica se instrução MUL não foi executada em um ciclo já
		if (tratamentoMul != 1) {
	        for (int i=4; i>0; i--) {
	            stages[i] = stages[i-1];
	        }
	        stages[0] = new Instruction();
	        tratamentoMul = 0;
		}
		else {
			// "Trava" a instrução MUL para execução em mais um ciclo e evita hazard estrutural
			stages[4] = stages[3];
			stages[3] = new Instruction();
			if (stages[1].getName()==null) {
				stages[1] = stages[0];
				stages[0] = new Instruction();
			}
			tratamentoMul++;
		}
		
		if (stages[2].getName()!=null && stages[2].getName().equals("MUL") && tratamentoMul==0) {
			tratamentoMul++;
		}
        cycle++;
    }

	// Método para verificação de hazards
	public boolean hasHazard(Instruction newInstr) {
		// Detecção de Hazard de Dados em tratamento com FORWARDING
		if (hazardType == HazardType.FORWARDING) {
			if (stages[1].getName() != null && stages[1].getName().equals("LW")) {
				if (stages[1].getRd()==newInstr.getRs() || stages[1].getRd()==newInstr.getRt()) {
					return true;
				}
			}
		}
		else {
			// Detecção de Hazard de Dados em tratamento com STALL
		    for (Instruction s : stages) {
		    	if (s.getRd() != null) {
			        if (s.getName() != null && s.getRd() == newInstr.getRs()) {
			            return true;
			        }
			        if (s.getName() != null && s.getRd() == newInstr.getRt()) {
			            return true;
			        }
		    	}
		    }
		}
	    return false;
	}
	
	// Simula o processo principal do Pipeline
	public void simulate(List<Instruction> program, int auto) {
		Scanner sc = new Scanner(System.in);
		
		// Segue o Pipeline enquanto o contador do programa não chega ao final e enquanto o os estágios não estão vazios
	    while (pc < program.size() || stages[0].getName() != null || stages[1].getName() != null
	    		|| stages[2].getName() != null || stages[3].getName() != null
	    		|| stages[4].getName() != null) {
	    	
	    	if (pc < program.size() && stages[0].getName()==null) {
	    		// Captura instrução atual do contador de programa
		        Instruction currentInstr = program.get(pc);
		        
		        // Predição de desvios
		        if (stages[1].getName() != null && stages[1].getName().equals("BEQ")) {
		        	if (handleBranch()) {
		        		stages[1].setPredict(true);
		        		
		        		// Desvia para a instrução da label referida
		        		try {
		        			pc = desvios.get(stages[1].getLabel());
		        		}
		        		catch (NullPointerException e) {
	        				throw new LabelException("Label inexistente!");
	        			}
		        		
		        		// Verifica Hazard e seta como NULL o estágio de IF quando retorna true 
		        		if (hasHazard(program.get(pc))) {
		    	            stages[0].setName(null);
		    	            stalls++;
		    	        }
		    	        else {
		    	            stages[0] = program.get(pc);
		    	            pc++;
		    	        }
		        	}
		        	else {
		        		if (hasHazard(currentInstr)) {
				            stages[0].setName(null);
				            stalls++;
				        }
				        else {
				            stages[0] = currentInstr;
				            pc++;
				        }
		        	}
		        }
		        else {
			        if (hasHazard(currentInstr)) {
			            stages[0].setName(null);
			            stalls++;
			        }
			        else {
			            stages[0] = currentInstr;
			            pc++;
			        }
		        }
	    	}
	    	
	        if (stages[3].getName() != null && stages[3].getName().equals("BEQ")) {
	        	// Verificação de ocorrência real de desvio no estágio EX
	        	if (stages[3].BEQ()) {
	        		if (!stages[3].getPredict()) {
	        			// Em caso de conflito de resultados com a predição, limpa o pipeline
	        			for (int i=0; i<3; i++) {
	        				stages[i] = new Instruction();
	        			}
	        			try {
	        				pc = desvios.get(stages[3].getLabel());
	        			}
	        			catch (NullPointerException e) {
	        				throw new LabelException("Label inexistente!");
	        			}
	        			stages[0] = program.get(pc);
	        			pc++;
	        		}
	        		else {
	        			truePredicts++;
	        		}
	        		// Atualiza estado para um nível acima, considerando como "mais provável" o desvio
	        		if (status == Status.STRONGLY_NOT_TAKEN) this.setStatus(Status.WEAKLY_NOT_TAKEN);
        			if (status == Status.WEAKLY_NOT_TAKEN) this.setStatus(Status.WEAKLY_TAKEN);
        			if (status == Status.WEAKLY_TAKEN) this.setStatus(Status.STRONGLY_TAKEN);
	        	}
	        	else {
	        		if (stages[3].getPredict()) {
	        			for (int i=0; i<3; i++) {
	        				stages[i] = new Instruction();
	        			}
	        			pc = stages[3].getPc()+1;
	        			if (pc < program.size()) stages[0] = program.get(pc);
	        			pc++;
	        		}
	        		else {
	        			truePredicts++;
	        		}
	        		// Atualiza estado para um nível abaixo, considerando como "menos provável" o desvio
	        		if (status == Status.WEAKLY_NOT_TAKEN) this.setStatus(Status.STRONGLY_NOT_TAKEN);
        			if (status == Status.WEAKLY_TAKEN) this.setStatus(Status.WEAKLY_NOT_TAKEN);
        			if (status == Status.STRONGLY_TAKEN) this.setStatus(Status.WEAKLY_TAKEN);
	        	}
	        	Main.qtdBeq++;
	        }
	        
	        // Desvio incondicional
	        if (stages[1].getName() != null && stages[1].getName().equals("J")) {
	        	try {
	        		pc = desvios.get(stages[1].J());
	        	}
	        	catch (NullPointerException e) {
    				throw new LabelException("Label inexistente!");
    			}
	        	stages[0] = program.get(pc);
	        	pc++;
	        }
	        
	        int temp=0;
	        if (hazardType == HazardType.STALL) {
	        	temp += 2;
	        }
	        
	        // Escrita no registrador na fase de WB
	        if (stages[2+temp].getName() != null) {
	        	switch (stages[2+temp].getName()) {
	        	
	        	case "ADD":
	        		stages[2+temp].ADD();
	        		break;
	        		
	        	case "ADDI":
	        		stages[2+temp].ADDI();
	        		break;
	        		
	        	case "SUB":
	        		stages[2+temp].SUB();
	        		break;
	        		
	        	case "MUL":
	        		if (temp==0 && tratamentoMul==1) {
	        			stages[2+temp].MUL();
	        		}
	        		break;
	        		
	        	case "LW":
	        		if (temp == 2) temp = 1;
	        		stages[3+temp].LW();
	        		break;
	        		
	        	}
	        }
	        
	        // Escrita na memória na fase de MEM
	        if (stages[3].getName() != null) {
	        	switch (stages[3].getName()) {
	        		
	        	case "SW":
	        		stages[3].SW();
	        		break;
	        		
	        	}
	        }
	        
	        // Imprime no terminal o Pipeline e avança o mesmo
	        printPipelineState(program);
	        advance();
	        
	        if (auto == 1) {
	        	try {
	        	    Thread.sleep(1000); // Pausa a execução em 1 segundo
	        	} catch (InterruptedException e) {
	        	    System.err.println("Thread interrompida: " + e.getMessage());
	        	}
	        }
	        else {
		        System.out.println(YELLOW + " 1)" + RESET + " Proximo ciclo.");
		        System.out.println(YELLOW + " 2)" + RESET + " Estado dos Registradores.");
		        System.out.print(YELLOW + " 3)" + RESET + " Estado da Memoria.\n\n * Escolha: ");
		        String option = sc.next();
		        
		        while (!option.equals("1")) {
		        	System.out.println();
		        	// Imprime na tela o estado dos Registradores
		        	if (option.equals("2")) {
		        		printRegisters();
		        		System.out.println(YELLOW + " 1)" + RESET + " Proximo ciclo.");
		    	        System.out.print(YELLOW + " 3)" + RESET + " Estado da Memoria.\n\n * Escolha: ");
		    	        option = sc.next();
		        	}
		        	else {
		        		// Imprime na tela o estado da Memória
		        		printMemory();
		        		System.out.println(YELLOW + " 1)" + RESET + " Proximo ciclo.");
		    	        System.out.print(YELLOW + " 2)" + RESET + " Estado dos Registradores.\n\n * Escolha: ");
		    	        option = sc.next();
		        	}
		        }
	        }
	    }
	    sc.close();
	}
	
	// Método para imprimir o estado do Pipeline
	public void printPipelineState(List<Instruction> program) {
		System.out.println("\n---------------------------------------------------");
	    System.out.println(YELLOW + " CICLO DE CLOCK: " + RESET + (cycle+1) + "\n");
	    String[] names = {" IF:  ", " ID:  ", " EX:  ", " MEM: ", " WB:  "};
	    for (int i=0; i<5; i++) {
	        System.out.print(names[i] + (stages[i].getName() != null ? GREEN + stages[i].toString() + RESET : RED + "NOP" + RESET));
	        if (i==0 && stages[i].getName()==null && pc<program.size() && tratamentoMul!=2) {
	        	System.out.print(RED + " (bolha inserida)" + RESET);
	        }
	        System.out.println();
	    }
	    System.out.println();
	    System.out.println("---------------------------------------------------");
	}
	
	// Método para imprimir o estado dos Registradores
	public static void printRegisters() {
		System.out.println("- - - - - - - - R E G I S T R A D O R E S - - - - - - - -\n");
	    
	    for (int i=0; i<8; i++) {
        	System.out.print("|" + BG_GREEN);
        	System.out.printf("%s%-2d"," $s", i);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", regs[i]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d"," $s", i+8);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", regs[i+8]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d"," $s", i+16);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", regs[i+16]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d"," $s", i+24);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", regs[i+24]);
        	System.out.println(" | ");
        }
	    System.out.println();
	}
	
	// Método para imprimir o estado da Memória
	public static void printMemory() {
		System.out.println("- - - - - - - - - - - - - M E M O R I A - - - - - - - - - - - - -\n");
	    
	    for (int i=0; i<8; i++) {
        	System.out.print("|" + BG_GREEN);
        	System.out.printf("%s%-2d","  ", i);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", memory[i]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d","  ", i+8);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", memory[i+8]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d","  ", i+16);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", memory[i+16]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d","  ", i+24);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", memory[i+24]);
        	System.out.print(" | " + BG_GREEN);
        	System.out.printf("%s%-2d","  ", i+32);
        	System.out.print(" " + RESET + ": ");
        	System.out.printf("%-2d ", memory[i+32]);
        	System.out.println(" | ");
        }
	    System.out.println();
	}
	
	// Controle da predição de Branch
	public boolean handleBranch() {
	    Random random = new Random();
	    // Define uma posição aleatória para um vetor auxiliar de probabilidade
	    int pos = random.nextInt(100)%4;
	    boolean taken = true;
	    
	    // Probabilidade de 25%
	    if (status.equals(Status.STRONGLY_NOT_TAKEN)) {
	    	boolean prob[] = {false, true, false, false};
	    	taken = prob[pos];
	    }
	    else {
	    	// Probabilidade de 50%
	    	if (status.equals(Status.WEAKLY_NOT_TAKEN)) {
	    		boolean prob[] = {false, true, false, true};
		    	taken = prob[pos];
	    	}
	    	else {
	    		// Probabilidade de 75%
	    		if (status.equals(Status.WEAKLY_TAKEN)) {
	    			boolean prob[] = {true, true, false, true};
	    	    	taken = prob[pos];
	    		}
	    	}
	    }

	    return taken;
	}
	
	// Getters e Setters
	public HazardType getHazard() {
		return hazardType;
	}
	
	public void setHazard(HazardType hazardType) {
		this.hazardType = hazardType;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public int getPc() {
		return pc;
	}

	public int getCycle() {
		return cycle;
	}

	public int getStalls() {
		return stalls;
	}
	
	public int getTruePredict() {
		return truePredicts;
	}
}
