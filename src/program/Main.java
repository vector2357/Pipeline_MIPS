package program;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import enums.HazardType;

/*
 * Projeto Simulador de Pipeline em processador MIPS
 * 
 * TURMA:       Arquitetura de Computadores II - A02
 * ALUNO:       Victor Hugo Resende Marinho
 * LINK GITHUB: https://github.com/vector2357/Pipeline_MIPS#
*/

public class Main {
	private static final String pipe = "\n\t\t---------------------------------------------------------------------------\n"
			+ "\t\t    .______    __  .______    _______  __       __  .__   __.  _______ \r\n"
			+ "\t\t    |   _  \\  |  | |   _  \\  |   ____||  |     |  | |  \\ |  | |   ____|\r\n"
			+ "\t\t    |  |_)  | |  | |  |_)  | |  |__   |  |     |  | |   \\|  | |  |__   \r\n"
			+ "\t\t    |   ___/  |  | |   ___/  |   __|  |  |     |  | |  . `  | |   __|  \r\n"
			+ "\t\t    |  |      |  | |  |      |  |____ |  `----.|  | |  |\\   | |  |____ \r\n"
			+ "\t\t    | _|      |__| | _|      |_______||_______||__| |__| \\__| |_______|\n\n"
			+ "\t\t---------------------------------------------------------------------------";	
	
	// Números de Branchs condicionais executados
	public static int qtdBeq = 0;
	
	public static void main(String[] args) {
		Scanner entrada = new Scanner(System.in);
		
		// Instância da classe Pipeline
		Pipeline pipeline = new Pipeline();
		// Lista responsavel por armazenar as instruções contidas no programa
		List<Instruction> program = new ArrayList<>();
		
		try {
			// Leitura do arquivo que contém o programa
			File file = new File("C:\\temp\\ws-eclipse\\PipeliningMIPS\\programa.txt");
			Scanner sc = new Scanner(file);
			int i=0;
			
			// Separa os parâmetros de cada instrução
            while (sc.hasNextLine()) {
            	String linha = sc.nextLine();
                String[] atributos = linha.split(" ");
                
                // Instancia uma instrução passando o vetor de parâmetros e a linha atual do programa
                Instruction instr = new Instruction(atributos, i);
                program.add(instr);
                i++;
            }
            
            // Definição do tipo de execução do Pipeline (manual ou automática)
            System.out.println(pipe + "\n");
            System.out.println("\n - Iniciando Processador MIPS com " + Pipeline.YELLOW + "Pipeline:" + Pipeline.RESET);
            System.out.println("\n\t* digite " + Pipeline.CIAN + "0" + Pipeline.RESET + " para uma execução " + Pipeline.CIAN + "manual." + Pipeline.RESET);
            System.out.println("\t* digite " + Pipeline.GREEN + "1" + Pipeline.RESET + " para uma execução " + Pipeline.GREEN + "automática." + Pipeline.RESET);
            int auto = entrada.nextInt();
            System.out.println();
            
            // Definição do tipo de tratamento de hazards que será usado no Pipeline (STALL ou FORWARDING)
            int hazard;
            System.out.println("\n\t* digite " + Pipeline.CIAN + "0" + Pipeline.RESET + " para tratamento de hazards com " + Pipeline.CIAN + "STALL." + Pipeline.RESET);
            System.out.println("\t* digite " + Pipeline.GREEN + "1" + Pipeline.RESET + " para tratamento de hazards com " + Pipeline.GREEN + "FORWARDING." + Pipeline.RESET);
            hazard = entrada.nextInt();
            
            // Define o HazardType com o status de tratamento a ser considerado pelo Pipeline
            if (hazard == 1) {
            	pipeline = new Pipeline(HazardType.FORWARDING);
            }
            else {
            	pipeline = new Pipeline(HazardType.STALL);
            }
            
            // Impressão do programa no terminal
            System.out.println("\n" + Pipeline.CIAN + " programa.txt:\n" + Pipeline.RESET);
            System.out.println("---------------------------------------------------");
            for (Instruction instr : program) {
            	System.out.println("-\t" + instr.toString());
            }
            System.out.println("---------------------------------------------------");

            System.out.print(Pipeline.YELLOW + "\n    * pressione alguma tecla e enter para continuar..." + Pipeline.RESET);
            
            String tecla = entrada.next();
            
            System.out.println(Pipeline.desvios.get("end:"));
            // Chamada do método de simulação do Pipeline
            pipeline.simulate(program, auto);
        }
		catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado!");
        }
		finally {
			entrada.close();
		}
		
		// Impressão dos resultados principais do Pipeline
		System.out.println("\n- - - - - - - R E S U L T A D O S - - - - - - -\n");
		System.out.println(Pipeline.CIAN + "Número de ciclos" + Pipeline.RESET + ": " + pipeline.getCycle());
		System.out.println(Pipeline.CIAN + "Número de bolhas inseridas" + Pipeline.RESET + ": " + pipeline.getStalls());
		if (qtdBeq != 0) {
			System.out.printf("%sTaxa de acerto de predição%s: %.2f %%\n", Pipeline.CIAN, Pipeline.RESET, (float)pipeline.getTruePredict()/qtdBeq*100.0);
		}
		System.out.println();
		Pipeline.printRegisters();
		System.out.println();
		Pipeline.printMemory();
	}

}
