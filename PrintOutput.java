public class PrintOutput {

	private int indent = 0;

	public void print(String s) {
		String spaces = "";
		for (int i = 0; i < indent; i++) {
			spaces += " ";
		}
		System.out.println(spaces + s);
	}

	public void indent() {
		indent += 2;
	}

	public void outdent() {
		indent -= 2;
	}

	public void newLine() {
		System.out.println();
	}

}