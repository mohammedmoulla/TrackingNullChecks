package trackingnullchecks;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Tracker {

    public static String readFile(String path) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String source = "";
        String line = br.readLine();
        
        if (line != null) {
            line = line.trim();
        }

        while (line != null) {
            line = line.trim();
            source += line + '\n';
            line = br.readLine();
        }
        br.close();
        return source;
    }

    public static void writeToFile(String path, String[] mat) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        for (String s : mat) {
            bw.write(s + "\n");
        }
        bw.close();
    }

    public static void printFile(String path) throws FileNotFoundException, IOException {
        System.out.println("file contains :");
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = br.readLine();
        while (line != null) {
            System.out.println(line);
            line = br.readLine();
        }
        br.close();
    }

    public static String[] splitSource(String source) {
        String mat[] = source.split("\n");
        String codeWithoutComments1 = Tracker.cleanCode(mat);
        mat = codeWithoutComments1.split("\n");
        return mat;
    }

    public static String cleanCode(String[] mat) {
        String result = "";
        boolean comment = false;
        for (String line : mat) {
            if (line.startsWith("//")) {
                continue;
            }
            if (line.startsWith("/*")) {
                comment = true;
            }
            if (line.endsWith("*/")) {
                comment = false;
                continue;
            }
            if (line.contains("//")) {
                line = line.substring(0, line.indexOf("//"));
            }
            if (!comment) {
                result += line + '\n';
            }
        }
        return result;
    }

    public static String[] difference(String[] mat1, String[] mat2) {
        int n1 = mat1.length;
        int n2 = mat2.length;
        List<String> res = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < n1) {
            if (mat1[i].equals(mat2[j])) {
                i++;
                j++;
                continue;
            }
            // now mat1 != mat2
            boolean found = false;
            int start = j;
            int end = -1;

            //search for mat1[i] in mat2
            //search for end 
            for (int p = j + 1; p < n2; p++) {
                if (mat1[i].equals(mat2[p])) {
                    end = p - 1;
                    found = true;
                    break;
                }
            }

            if (found) {
                // add difference
                for (int p = start; p <= end; p++) {
                    res.add("/*added---->*/ " + mat2[p]);
                }
                j = end + 2;
                i++;
            } else {
                //add mat1[i]
                res.add(mat1[i] + "/*---->deleted*/");
                i++;
            }
            //exit from while
            if (i == n1) {
                break;
            }
        }//end of while

        //add the remaining
        if (j != n2) {
            for (int p = j; p < n2; p++) {
                res.add("/*remained---->*/" + mat2[p]);
            }
        }
        
        int count = res.size();
        String[] result = new String[count];
        for (int p = 0; p < count; p++) {
            result[p] = res.get(p);
        }
        return result;
    }

    public static Map<Integer, String> get_Null_Checks(String[] mat) {
        Map<Integer, String> map = new HashMap<>();
        int size = mat.length;
        //pass for all the lines to find NC
        for (int k = 0; k < size; k++) {

            String m = mat[k];

            if (m.contains("null")) {
                int start = -1;
                int end = -1;
                 //we need to find start and end of the null check
                //if options
                if (m.contains("if")) {
                    start = m.indexOf("(") + 1;
                    end = -1;
                    //passing from end the line to the start 
                    //to handle if (s.print() != null) {
                    for (int i = m.length() - 1; i > 0; i--) {
                        if (m.charAt(i) == ')') {
                            end = i;
                            break;
                        }
                    }
                } //assignmenet options
                else if (m.contains("=")) {
                     //we have x = (s!=null) ;
                    //we cut  (s!=null)
                    start = m.indexOf("=") + 1;
                    end = m.indexOf(";");
                    m = m.substring(start, end);
                    //delete whitespaces from right and left
                    m = m.trim();
                     // get the index of last char to know if there is () 

                    if (m.charAt(0) == '(' && m.charAt(m.length() - 1) == ')') {
                        //we have (.......) #########################
                        //(s!=null)
                        //(s.print()!=null)
                        start = m.indexOf("(") + 1;
                        for (int i = m.length() - 1; i > 0; i--) {
                            if (m.charAt(i) == ')') {
                                end = i;
                                break;
                            }
                        }
                    } else { //we dont have () $$$$$$$$$$$$$$$$$$$$$$
                        //s != null
                        //s.print() != null
                        start = 0;
                        end = m.length();
                    }//end of assignement options

                } else //we dont have condition or assignmenet
                {
                    continue;
                }
                 //we have start , end
                //now you can cut the string from start --> end

                if (start == -1 || end == -1) {
                    continue;
                }

                String s = m.substring(start, end);
                s = s.replaceAll("\\s", "");
                 //s!=null --> s 
                //s==null -->s
                //s.print() != null -->s.print()
                //s.print() == null -->s.print()
                map.put(k, s);
            }
        }
        return map;
    }

    public static String get_NC(String NullCheck) {
        // s != null --> s
        String s = NullCheck;
        String temp = "";
        int k;
        if (s.contains("!")) {
            k = s.indexOf("!");
            s = s.substring(0, k);
        } else if (s.contains("=")) {
            k = s.indexOf("=");
            s = s.substring(0, k);
        }
        s = s.trim();
        temp = s;
        return temp;
    }

    public static Map.Entry<Integer, String> get_NC_Declaration(Map.Entry<Integer, String> entry, String[] code) {
         //s --> String s;

        int line_number = entry.getKey();
        String NC = get_NC(entry.getValue());
        
        String Declaration = "$";
        int declaration_line = -1;

        //we suppose that we dont see the NC yet
        boolean found = false;
         //search all the code to find the NC to know the declaration

        for (int i = line_number - 1; i >= 0; i--) {
            String original = code[i];
            String withoutSpaces = original.replaceAll(" ", "");

            if (withoutSpaces.contains(NC + ";")
                    || withoutSpaces.contains(NC + ",")
                    || withoutSpaces.contains(NC + ")")
                    || withoutSpaces.contains(NC + "=")) {

                int NC_index = original.indexOf(NC);
                for (int j = NC_index - 1; j >= 0; j--) {
                    if (Character.isUpperCase(original.charAt(j))) {
                        int Type_index = j;
                        String Type_name = original.substring(Type_index, NC_index - 1);
                        Declaration = Type_name + " " + NC;
                        declaration_line = i;
                        found = true;
                        break;
                    }//end of if
                }//end of for {j} 

            }//end of if 
            //if you find the NC --> break the whole loop
            if (found) {
                break;
            }
        }//end of for {i}

        Map<Integer, String> map = new HashMap<>();
        map.put(declaration_line, Declaration);
        Map.Entry result = map.entrySet().iterator().next();

        return result;
    }//end of method Get_NC_Declaration

    public static String get_Type(Map.Entry<Integer, String> entry, String[] code) {
         //return member or parameter or local 

        boolean member = false;
        boolean parameter = false;
        boolean local = false;
        String result = "Unknown";

        int line_number = entry.getKey();
        String Declaration = entry.getValue();

        //if the NC Declaration is not exist --> don't search for the type
        if (line_number == -1 || Declaration.equals("$")) {
            return result;
        }

        String declaration_line = code[line_number];

        //search to know if it is parameter
        int start = declaration_line.indexOf(Declaration);
        int end = start + Declaration.length();
        boolean right = false;
        boolean left = false;

        //searching for ( in the right side 
        for (int i = start - 1; i >= 0; i--) {
            if (declaration_line.charAt(i) == '(') {
                right = true;
                break;
            }
        }//end of for
        //searching for ) in the left side 
        for (int i = end; i < declaration_line.length(); i++) {
            if (declaration_line.charAt(i) == ')') {
                left = true;
                break;
            }
        }//end of for
        if (right && left) {
            parameter = true;
            result = "PARAMETER";
        }

         //we already know that it isn't parameter
        //search to know if it is local
        if (!parameter) {
            for (int i = line_number - 1; i >= 0; i--) {

                String line = code[i];
                line = line.replaceAll(" ", "");

                if (line.contains("){")
                        && !line.contains("for")
                        && !line.contains("while")
                        && !line.contains("do")) {
                    local = true;
                    result = "LOCAL";
                    break;
                } //end of if 

                if (line.contains("{") && line.contains("class")) {
                    member = true;
                    result = "MEMBER";
                    break;
                }//end of if

            }//end of for 

        }//end of if {!parameter}
        return "Type of " + Declaration + " is " + result;
    }

    public static Map<Integer, String> get_Def_Expressions(Map.Entry<Integer, String> declaration, Map.Entry<Integer, String> nullcheck, String[] code) {

        Map<Integer, String> map = new HashMap<>();

        String name = get_NC(nullcheck.getValue());

        int declaration_line = declaration.getKey();
        int null_check_line = nullcheck.getKey();

        for (int i = declaration_line; i < null_check_line; i++) {
            String line = code[i];
            if (line.contains("=") && line.contains(name)) {
                map.put(i, line);
            }
        }
        return map;
    }

    public static String get_Def_Value(String expression) {
         //input = String a = "hello";
        //output = "hello"

        int start = expression.indexOf("=");
        String result = "";

        for (int i = start + 1; i < expression.length(); i++) {
            if (expression.charAt(i) == ',' || expression.charAt(i) == ';') {
                break;
            }
            result += expression.charAt(i);
        }
        return result.trim();
    }//end of method get_Def_Value 

    public static void printReport(String source1_path, String source2_path, String destination_path, String report_path) throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(report_path));

        String source1 = Tracker.readFile(source1_path);
        String source2 = Tracker.readFile(source2_path);

        String mat1[] = Tracker.splitSource(source1);
        String mat2[] = Tracker.splitSource(source2);

        String[] difference = Tracker.difference(mat1, mat2);

        Tracker.writeToFile(destination_path, difference);

        Map<Integer, String> nullChecks = Tracker.get_Null_Checks(mat2);

        for (Map.Entry nullcheck : nullChecks.entrySet()) {

            bw.write("line = " + ((int) nullcheck.getKey() + 1) + " nullcheck = " + nullcheck.getValue() + "\n");

            Map.Entry<Integer, String> declaration = Tracker.get_NC_Declaration(nullcheck, mat2);

            bw.write("line = " + (declaration.getKey() + 1) + " declaration = " + declaration.getValue() + "\n");

            bw.write(Tracker.get_Type(declaration, mat2) + "\n");

            bw.write("*********expressions*********" + "\n");

            Map<Integer, String> defExpressions = Tracker.get_Def_Expressions(declaration, nullcheck, mat2);

            for (Map.Entry defExpression : defExpressions.entrySet()) {
                bw.write("line = " + ((int) defExpression.getKey() + 1) + " expression = " + defExpression.getValue() + "\n");
                bw.write("Value = " + Tracker.get_Def_Value((String) defExpression.getValue()) + "\n");
            }//end of for {defexpressions}

            bw.write("______________________________" + "\n");
        }//end of for {nullchecks}

        bw.close();
    }

}//end of class Tracker

public class TrackingNullChecks {

    public static void main(String[] args) throws FileNotFoundException, IOException {

        String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
        
        
        String source1_path =  currentPath+"\\files\\source1.txt";
        String source2_path = currentPath+"\\files\\source2.txt";
        String destination_path = currentPath+"\\files\\destination.txt";
        String report_path = currentPath+"\\files\\report.txt";

        Tracker.printReport(source1_path, source2_path, destination_path, report_path);
        Tracker.printFile(report_path);

    }//end of main
}//end of class 
