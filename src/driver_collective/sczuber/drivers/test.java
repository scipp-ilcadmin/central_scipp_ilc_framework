public class test{
    static double global = 0;
    public static void main(String[] args){
        System.out.println(global);
        global+=1;
        System.out.println(global);
        double local = 5;
        if(local > 2){
            global+=1;
            System.out.println(global);
        }
    }
}
