import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

class UsageException extends Exception {
	private static final long serialVersionUID = 1L;

	public UsageException() {
		super(
				"UsageException:\nUsage: java Main [options]"
						+ "-a algorithm Defaults to 0"
						+ "-i inputDirPath Defaults to ./input1"
						+ "-o outputDirPath Defaults to output1. Must have write priviledges here\n\n"
						+ " Input folder should have:\n  client.csv\n  nodes.csv\n  taxis.csv\n  lines.csv\n  traffic.csv\n"
						+ " ./common files should have: all prolog files");
	}
}

public class Main {
	private static String userdir;
	private static String inputDirPath;
	private static String outputDirPath;
	private static int algorithm;
	private static PrologParser myPrologParser;

	private static Hashtable<Long,Node> nodes;
	 
	static private PrintWriter pwNodes=null;
	static private PrintWriter pwNext=null;
	static private PrintWriter pwRest=null;
	private static PrintWriter pwLines=null;
	private static PrintWriter pwTraffic=null;
	private static ArrayList<Long> arr;
	
	private static BufferedReader newBuff(String name)
			throws UnsupportedEncodingException, FileNotFoundException {
		String charset = "ISO-8859-7";
		InputStreamReader isr;
		isr = new InputStreamReader(new FileInputStream(inputDirPath + "/"
				+ name + ".csv"), charset);
		BufferedReader in = new BufferedReader(isr);
		return in;
	}

	private static void read_data() throws UnsupportedEncodingException,
			IOException, UsageException {
		String linestr;
		String[] line;
		Scanner sc = null;
		BufferedReader in = null;
		/*
		 * Taxi taxi; Node node; Line line_road; Traffic traffic;
		 */
		// CLIENT
		sc = new Scanner(new File(inputDirPath + "/client.csv"));
		sc.next();
		linestr = sc.next();
		int num=myPrologParser.num("client");
		line = linestr.split(",",num);
		myPrologParser.writePrologFact(pwRest,"client", line);
		sc.close();
		
		// TAXIS
		sc = new Scanner(new File(inputDirPath + "/taxis.csv"));
		sc.next();
		sc.next();
		while (sc.hasNext()) {
			linestr = sc.next();
			sc.skip("	");
			String linestr2 = "";
			String tab = sc.next();
			// taxis end with "\t(Name Name)\n
			tab = tab.substring(1, tab.length());
			while (!(tab.contains(")"))) {
				linestr2 = linestr2+tab+" ";
				tab = sc.next();
			}
			if (tab != null && tab.length() > 1)
				tab = tab.substring(0, tab.length() - 1);
			linestr2 = linestr2 + tab;
			linestr += ",";
			linestr+=linestr2;
			num=myPrologParser.num("taxi");
			line = linestr.split(",",num);
			myPrologParser.writePrologFact(pwRest,"taxi", line);
		}
		sc.close();
		pwRest.close();
		
		// LINES
		in = newBuff("lines");
		linestr = in.readLine();
		while (linestr != null) {
			num=myPrologParser.num("line");
			line = linestr.split(",",num);
			myPrologParser.writePrologFact(pwLines,"line", line);
			linestr = in.readLine();
		}
		in.close();
		pwLines.close();

		// TRAFFIC
		in = newBuff("traffic");
		linestr = in.readLine();
		while (linestr != null) {
			num=myPrologParser.num("traffic");
			line = linestr.split(",",num);
			myPrologParser.writePrologFact(pwTraffic, "traffic", line);
			linestr = in.readLine();
		}	
		in.close();
		pwTraffic.close();
		
		PrologParser.consult(outputDirPath+"/lines.pl");
//		PrologParser.consult(outputDirPath+"/taxis.pl");
		
		// NODES
		arr=new ArrayList<Long>();
		nodes = new Hashtable<Long, Node>();
		myPrologParser.setNodes(nodes);
		in = newBuff("nodes");
		linestr = in.readLine();
		linestr = in.readLine();
		while (linestr != null) {
			num=myPrologParser.num("node");
			line = linestr.split(",",num);
			long l=new Long(line[3]);
			
			if (nodes.get(l)==null){
				nodes.put(l,new Node(l));
				arr.add(l);
			}
			myPrologParser.writePrologFact(pwNodes,"node", line);
			if(nodes.get(l).hScore==0.0D){
				System.out.println(l);
				System.exit(1);
			}
			linestr = in.readLine();
		}
		in.close();
		pwNodes.close();
		pwNext.close();
	}

	private static void _read_data_() throws UsageException {
		try {
			read_data();
		} catch (UnsupportedEncodingException e) {
			System.out.println(e.getMessage());
			throw new UsageException();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			throw new UsageException();
		}
	}

	public static void main(String[] args) {
//		userdir = System.getProperty("user.dir");
		userdir=new java.io.File("").getAbsolutePath();
		System.out.println("userdir="+userdir);
		inputDirPath = userdir+"/input1";
		outputDirPath = userdir+"/output1";
		algorithm=0;
		try {
			// find i/o paths
			if (args.length == 0) {} 
			else if (args.length == 2 || args.length == 4 || args.length == 6) {
				for (int i=0; i<args.length; i+=2){	
				if (args[i].equals("-a"))
					algorithm=Integer.parseInt(args[i+1]);
				else if (args[i].equals("-i"))
					inputDirPath=args[i+1];
				else if (args[i].equals("-0"))
					outputDirPath=args[i+1];
				else
					throw new UsageException();
				}
			}
			else
				throw new UsageException();

			// create outputs folder
			try {
				File outdir;
				if (Files.exists(Paths.get(outputDirPath))){
					outdir = new File(outputDirPath);
					for (File c : outdir.listFiles())
					      Files.delete(Paths.get(c.getAbsolutePath()));
					(new File(outputDirPath)).delete();
				}
				else{
					outdir = new File(outputDirPath);
				}
				outdir.mkdirs();
			} catch (SecurityException s) {
				s.printStackTrace();
				throw new UsageException();
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new UsageException();
			}
			
			// copy genesis.pl to rules.pl and open new file facts.pl to write
			try {
				File from = new File("common-files/discontiguous.pl");
				File to = new File(outputDirPath + "/disc.pl");
				Files.copy(from.toPath(), to.toPath());
				
				from = new File("common-files/rules.pl");
				to = new File(outputDirPath + "/rules.pl");
				Files.copy(from.toPath(), to.toPath());
				
				from = new File("common-files/all.pl");
				to = new File(outputDirPath + "/all.pl");
				Files.copy(from.toPath(), to.toPath());
				
				to = new File(outputDirPath + "/nodes.pl");
				FileWriter fw = new FileWriter(to, true);
				BufferedWriter bw = new BufferedWriter(fw);
				pwNodes =new PrintWriter(bw);
				
				to = new File(outputDirPath + "/nextt.pl");
				fw = new FileWriter(to, true);
				bw = new BufferedWriter(fw);
				pwNext =new PrintWriter(bw);
				
				to = new File(outputDirPath + "/rest.pl");
				fw = new FileWriter(to, true);
				bw = new BufferedWriter(fw);
				pwRest =new PrintWriter(bw);
				
				to = new File(outputDirPath + "/lines.pl");
				fw = new FileWriter(to, true);
				bw = new BufferedWriter(fw);
				pwLines =new PrintWriter(bw);
				
				to = new File(outputDirPath + "/traffic.pl");
				fw = new FileWriter(to, true);
				bw = new BufferedWriter(fw);
				pwTraffic =new PrintWriter(bw);
				myPrologParser=new PrologParser(nodes,pwNext);
			} catch (IOException e) {
				e.printStackTrace();
				throw new UsageException();
			}

			System.out
					.println("################################################");
			System.out
					.println("#########   WELCOME TO  E-TARIFAS    ###########");
			System.out
					.println("################################################");

			System.out.println("Creating prolog files from data...");
			_read_data_();
			
			System.out.println("DONE\n");
			
			if(arr==null){
				System.out.println("arr==null");
				System.exit(1);
			}
			Asolver solver = new Asolver(inputDirPath,outputDirPath,nodes,PrologParser.jip, PrologParser.parser,arr,algorithm);
			
			solver.solve();
					} 
			catch (UsageException e) {
				e.printStackTrace();
				System.exit(0);
			}
	}
}
