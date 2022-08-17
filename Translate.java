import java.io.IOException;

import cs132.vapor.ast.VAddr;
import cs132.vapor.ast.VAssign;
import cs132.vapor.ast.VBranch;
import cs132.vapor.ast.VBuiltIn;
import cs132.vapor.ast.VCall;
import cs132.vapor.ast.VGoto;
import cs132.vapor.ast.VInstr;
import cs132.vapor.ast.VLabelRef;
import cs132.vapor.ast.VLitInt;
import cs132.vapor.ast.VMemRead;
import cs132.vapor.ast.VMemRef;
import cs132.vapor.ast.VMemWrite;
import cs132.vapor.ast.VOperand;
import cs132.vapor.ast.VReturn;
import cs132.vapor.ast.VVarRef;

public class Translate extends VInstr.Visitor<IOException> {

	PrintOutput p;
	public boolean print = false;
	public boolean heap = false;
	public boolean error1 = false;
	public boolean error2 = false;

	public Translate(PrintOutput p) {
		this.p = p;
	}

	@Override
	public void visit(VAssign v) throws IOException {
		String s = "";
		if (v.dest instanceof VVarRef.Local) {
			s += v.dest.toString();
		} else if (v.dest instanceof VVarRef.Register) {
			s += v.dest.toString();
		}
		if (v.source instanceof VLitInt) {
			s += " " + v.source.toString();
			s = "li " + s;
		} else if (v.source instanceof VLabelRef) {
			s += " " + ((VLabelRef) v.source).ident;
			s = "la " + s;
		} else {
			s += " " + v.source.toString();
			s = "move " + s;
		}

		p.print(s);
	}

	@Override
	public void visit(VCall v) throws IOException {
		if (v.addr instanceof VAddr.Label) {
			VLabelRef label = (VLabelRef) ((VAddr.Label) v.addr).label;
			p.print("jal " + label.ident);
		} else {
			p.print("jalr " + v.addr.toString());
		}

	}

	@Override
	public void visit(VBuiltIn v) throws IOException {
		if (v.op != null) {
			String call = "";
			if (v.op.name == "HeapAllocZ") {
				heap = true;
				for (VOperand vO : v.args) {
					if (vO instanceof VLitInt) {
						p.print("li $a0 " + vO.toString());
					}
					if (vO instanceof VVarRef) {
						p.print("move $a0 " + vO.toString());
					}
				}
				call = "_heapAlloc";
				p.print("jal " + call);
			} else if (v.op.name == "Error") {
				if (v.args[0].toString().contains("null")) {
					error1 = true;
					p.print("la $a0 _str0");
				} else {
					error2 = true;
					p.print("la $a0 _str1");
				}

				call = "_error";
				p.print("j " + call);
			} else if (v.op.name == "PrintIntS") {
				print = true;
				for (VOperand vO : v.args) {
					if (vO instanceof VLitInt) {
						p.print("li $a0 " + vO.toString());
					}
					if (vO instanceof VVarRef) {
						p.print("move $a0 " + vO.toString());
					}
				}
				call = "_print";
				p.print("jal " + call);
			} else if (v.op.name == "LtS") {
				String first = v.dest.toString();
				String second = v.args[0].toString();
				String third = v.args[1].toString();
				if (v.args[1] instanceof VVarRef.Register) {
					call = "slt ";
				} else {
					call = "slti ";
				}
				p.print(call + first + " " + second + " " + third);
				return;
			} else if (v.op.name == "Sub") {
				String first = v.dest.toString();
				String second = v.args[0].toString();
				if (v.args[0] instanceof VLitInt) {
					if (v.args[1] instanceof VLitInt) {
						int sub = ((VLitInt) v.args[0]).value - ((VLitInt) v.args[1]).value;
						p.print("li " + first + " " + sub);
						return;
					}
					p.print("li $t9 " + v.args[0].toString());
					second = "$t9";
				}

				String third = v.args[1].toString();
				p.print("subu " + first + " " + second + " " + third);
				return;
			} else if (v.op.name == "MulS") {
				String first = v.dest.toString();
				if (v.args[0] instanceof VLitInt && v.args[1] instanceof VLitInt) {
					int mul = ((VLitInt) v.args[0]).value * ((VLitInt) v.args[1]).value;
					p.print("li " + first + " " + mul);
					return;
				}
				String second = v.args[0].toString();
				String third = v.args[1].toString();
				if (v.args[0] instanceof VLitInt) {
					second = third;
					third = v.args[0].toString();
				}

				p.print("mul " + first + " " + second + " " + third);
				return;
			} else if (v.op.name == "Add") {
				String first = v.dest.toString();
				String second = v.args[0].toString();
				String third = v.args[1].toString();
				p.print("addu " + first + " " + second + " " + third);
				return;
			} else if (v.op.name == "Lt") {
				String first = v.dest.toString();
				String second = v.args[0].toString();
				String third = v.args[1].toString();
				if (v.args[0] instanceof VLitInt) {
					p.print("li $t9 " + second);
					second = "$t9";
				}
				call = "sltu ";
				p.print(call + first + " " + second + " " + third);
				return;
			}
		}

		if (v.dest != null) {
			p.print("move " + v.dest.toString() + " $v0");
		}

	}

	@Override
	public void visit(VMemWrite v) throws IOException {
		String call = "";
		String source = "";
		String dest = "";
		String offset = "";
		boolean isInt = false;

		if (v.source instanceof VLitInt) {
			isInt = true;
			if (((VLitInt) v.source).value > 0) {
				p.print("li $t9 " + ((VLitInt) v.source).value);
				source = "$t9";
			} else {
				source = "$" + v.source.toString();
			}
			call = "sw";
		}
		if (v.source instanceof VOperand.Static) {

		}
		if (v.source instanceof VVarRef.Register) {
			call = "sw";
			source = v.source.toString();
		}
		if (v.source instanceof VLabelRef) {
			call = "sw";
			source = ((VLabelRef) v.source).ident;
			p.print("la $t9 " + source);
			source = "$t9";
		}

		if (v.dest instanceof VMemRef.Stack) {
			VMemRef.Stack stack = (VMemRef.Stack) v.dest;
			int index = stack.index * 4;
			if (stack.region.name() == "Local") {
				dest = "$sp";
				offset += index;
			}
			if (stack.region.name() == "In") {
				p.print("li $t9 " + v.source.toString());
			}
			if (stack.region.name() == "Out") {
				if (isInt) {
					p.print("li $t9 " + v.source.toString());
					offset += index;
					dest = "$sp";
					source = "$t9";
				} else {
					dest = "$sp";
					offset += index;
				}

			}
		}
		if (v.dest instanceof VMemRef.Global) {
			VMemRef.Global vmem = (VMemRef.Global) v.dest;
			VAddr vadd = (VAddr) vmem.base;
			dest = vadd.toString();

			int oset = vmem.byteOffset;
			offset += oset;
		}
		p.print(call + " " + source + " " + offset + "(" + dest + ")");

		/*
		 * if (v.source != null) { if (v.source instanceof VLabelRef) {
		 * p.print("la $t9 " + ((VLabelRef) v.source).ident); } else if (v.source
		 * instanceof VLitInt) { p.print("li $t9 " + v.source.toString()); if (v.dest
		 * instanceof VMemRef.Stack) { int index = ((VMemRef.Stack) v.dest).index * 4;
		 * p.print("sw " + v.source.toString() + " " + index + "($sp)"); } } else if
		 * (v.source instanceof VVarRef.Register) { String s; if (v.dest instanceof
		 * VMemRef.Stack) { int index = ((VMemRef.Stack) v.dest).index * 4;
		 * p.print("sw " + v.source.toString() + " " + index + "($sp)"); }
		 * 
		 * } } if (v.dest != null) { if (v.source instanceof VLabelRef) { if (v.dest
		 * instanceof VMemRef.Global) { VMemRef.Global vmem = (VMemRef.Global) v.dest;
		 * VAddr vadd = (VAddr) vmem.base; p.print("sw $t9 " + vmem.byteOffset + "(" +
		 * vadd.toString() + ")"); } } else { if (v.dest instanceof VMemRef.Global) {
		 * VMemRef.Global vmem = (VMemRef.Global) v.dest; VAddr vadd = (VAddr)
		 * vmem.base; String word = ""; if (v.source instanceof VLitInt) { word = "$" +
		 * v.source.toString(); } else { word = v.source.toString(); } p.print("sw " +
		 * word + " " + vmem.byteOffset + "(" + vadd.toString() + ")"); } }
		 * 
		 * }
		 */

	}

	@Override
	public void visit(VMemRead v) throws IOException {
		// TODO Auto-generated method stub
		if (v.source instanceof VMemRef.Global) {
			VMemRef.Global vmem = (VMemRef.Global) v.source;
			VAddr vadd = (VAddr) vmem.base;
			p.print("lw " + v.dest.toString() + " " + vmem.byteOffset + "(" + vadd.toString() + ")");
		} else if (v.source instanceof VMemRef.Stack) {
			VMemRef.Stack vmem = (VMemRef.Stack) v.source;
			int index = vmem.index * 4;
			String proc = "($sp)";
			if (vmem.region.toString().equals("In")) {
				proc = "($fp)";
			}
			p.print("lw " + v.dest.toString() + " " + index + proc);
		}

	}

	@Override
	public void visit(VBranch v) throws IOException {
		if (v.positive == true) {
			if (v.value instanceof VVarRef.Register) {
				p.print("bnez " + v.value.toString() + " " + ((VLabelRef) v.target).ident);

			}

		} else {
			p.print("beqz " + v.value.toString() + " " + ((VLabelRef) v.target).ident);
		}

	}

	@Override
	public void visit(VGoto v) throws IOException {
		VAddr.Label add = ((VAddr.Label) v.target);

		p.print("j " + add.label.ident);

	}

	@Override
	public void visit(VReturn v) throws IOException {
		// TODO Auto-generated method stub

	}

}