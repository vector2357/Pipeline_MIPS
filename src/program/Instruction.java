package program;

import exceptions.InstructionNameException;

public class Instruction {

	// Nome da instrução e da label (em caso de instruções com label)
	private String name, label;
	// Registradores de entrada (rt, rs) e destino (rd)
	private Integer rs, rt, rd;
	// Valor imediato para instruções imediatas
	private Integer immediate;
	// Atributo que armazena resultado da predição (para instrução BEQ)
	private Boolean predict;
	// Armazena a linha para impressão da instrução no terminal
	private String[] linha = new String[5];
	// Posição em que se encontra a instrução no programa
	private int pc;
	
	public Instruction() {
		this.name = null;
	}
	
	public Instruction(String[] params, int pc) {
		this.linha = params;
		this.predict = false;
		this.pc = pc;
		int i, aux=0;
		
		// Definição de parâmetro para linha com label
		if (params[0].charAt(params[0].length()-1) == ':') {
			this.label = params[0];
			Pipeline.desvios.put(this.label, pc);
			aux++;
		}
		this.name = params[0+aux].toUpperCase();
		// Instrução NOP (equivale ao nome da instrução definido como NULL)
		if (name.equals("NOP")) {
			this.name = null;
		}
		else {
			// Inicialização de parâmetros para instruções de acesso à memória
			if (name.equals("LW") || name.equals("SW")) {
				this.immediate = 0;
				
				// LW faz uso do registrador de destino (rd)
				if (name.equals("LW")) {
					this.rd = 0;
					for (i=2; i<params[1+aux].length()-1; i++) {
						this.rd *= 10;
						this.rd += params[1+aux].charAt(i) - '0';
					}
				}
				else { // SW faz uso do registrador de entrada (rt)
					this.rt = 0;
					for (i=2; i<params[1+aux].length()-1; i++) {
						this.rt *= 10;
						this.rt += params[1+aux].charAt(i) - '0';
					}
				}
				for (i=0; i<params[2+aux].length() && params[2+aux].charAt(i) != '('; i++) {
					this.immediate *= 10;
					this.immediate += params[2+aux].charAt(i) - '0';
				}
				i+=3;
				this.rs = 0;
				for (; i<params[2+aux].length() && params[2+aux].charAt(i) != ')'; i++) {
					this.rs *= 10;
					this.rs += params[2+aux].charAt(i) - '0';
				}
			}
			else {
				// Parâmetros para as instruções ADD, SUB e MUL
				if (name.equals("ADD") || name.equals("SUB") || name.equals("MUL")) {
					this.rd = 0;
					for (i=2; i<params[1+aux].length()-1; i++) {
						this.rd *= 10;
						this.rd += params[1+aux].charAt(i) - '0';
					}
					this.rs = 0;
					for (i=2; i<params[2+aux].length()-1; i++) {
						this.rs *= 10;
						this.rs += params[2+aux].charAt(i) - '0';
					}
					this.rt = 0;
					for (i=2; i<params[3+aux].length(); i++) {
						this.rt *= 10;
						this.rt += params[3+aux].charAt(i) - '0';
					}
				}
				else {
					// Parâmetros para a instrução de desvio condicional BEQ
					if (name.equals("BEQ")) {
						this.rt = 0;
							for (i=2; i<params[1+aux].length()-1; i++) {
							this.rt *= 10;
							this.rt += params[1+aux].charAt(i) - '0';
						}
						this.rs = 0;
						for (i=2; i<params[2+aux].length()-1; i++) {
							this.rs *= 10;
							this.rs += params[2+aux].charAt(i) - '0';
						}
						var sb = new StringBuilder(params[3+aux]);
						sb.append(':');
						this.label = sb.toString();
					}
					else {
						// Parâmetros para a instrução ADDI
						if (name.equals("ADDI")) {
							this.rd = 0;
							for (i=2; i<params[1+aux].length()-1; i++) {
								this.rd *= 10;
								this.rd += params[1+aux].charAt(i) - '0';
							}
							this.rt = 0;
							for (i=2; i<params[2+aux].length()-1; i++) {
								this.rt *= 10;
								this.rt += params[2+aux].charAt(i) - '0';
							}
							this.immediate = 0;
							for (i=0; i<params[3+aux].length(); i++) {
								this.immediate *= 10;
								this.immediate += params[3+aux].charAt(i) - '0';
							}
						}
						else {
							if (!name.equals("J")) {
								throw new InstructionNameException("Instrução " + params[0+aux] + ", linha " + (pc+1) + " está incorreta!");
							}
							// Instrução de desvio incondicional (J)
							var sb = new StringBuilder(params[1+aux]);
							sb.append(':');
							this.label = sb.toString();
						}
					}
				}			
			}
		}
	}
	
	public void ADD() {
		// RD = RT + RS
		Pipeline.regs[this.rd] = Pipeline.regs[this.rs] + Pipeline.regs[this.rt];
	}
	
	public void ADDI() {
		// RD = RT + IMMEDIATE
		Pipeline.regs[this.rd] = Pipeline.regs[this.rt] + this.immediate;
	}
	
	public void SUB() {
		// RD = RT - RS
		Pipeline.regs[this.rd] = Pipeline.regs[this.rs] - Pipeline.regs[this.rt];
	}
	
	public void MUL() {
		// RD = RT * RS
		Pipeline.regs[this.rd] = Pipeline.regs[this.rs] * Pipeline.regs[this.rt];
	}
	
	public void LW() {
		// REGS[RD] = MEMORY[REGS[RS] + IMMEDIATE]
		Pipeline.regs[this.rd] = Pipeline.memory[Pipeline.regs[this.rs] + this.immediate];
	}
	
	public void SW() {
		// MEMORY[REGS[RS] + IMMEDIATE] = REGS[RT]
		Pipeline.memory[Pipeline.regs[this.rs] + this.immediate] = Pipeline.regs[this.rt];
	}
	
	public boolean BEQ() {
		// if (REGS[RS] == REGS[RT])
		return Pipeline.regs[this.rs.intValue()] == Pipeline.regs[this.rt.intValue()];
	}
	
	public String J() {
		// retorna label a ser desviada pelo J
		return this.label;
	}

	// Getters e Setters	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Integer getRs() {
		return rs;
	}

	public void setRs(Integer rs) {
		this.rs = rs;
	}

	public Integer getRt() {
		return rt;
	}

	public void setRt(Integer rt) {
		this.rt = rt;
	}

	public Integer getRd() {
		return rd;
	}

	public void setRd(Integer rd) {
		this.rd = rd;
	}

	public Integer getImmediate() {
		return immediate;
	}

	public void setImmediate(Integer immediate) {
		this.immediate = immediate;
	}
	
	public Boolean getPredict() {
		return predict;
	}
	
	public void setPredict(Boolean predict) {
		this.predict = predict;
	}
	
	public int getPc() {
		return pc;
	}
	
	public void setPc(int pc) {
		this.pc = pc;
	}
	
	@Override
	public String toString() {
		var sb = new StringBuilder("");
		
		for (int i=0; i<linha.length; i++) {
			sb.append(linha[i]);
			sb.append(' ');
		}
		return sb.toString();
	}
}
