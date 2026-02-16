package task.Pet;

/**
 * 
 * @author ducco
 */
public class petTask {
    public petTaskTemplate template;
    public int id;
    public int count;
    public int countMax;
    public String  name;
    public int rewad;
    
    public petTask() {
        this.id = -1;
        this.count = 0;
        this.countMax = 0; 
        this.name = "";
        this.rewad = 0;
    }


    public petTask(int id, String name, int count, int countMax) {
        this.id = id;
        this.count = count;
        this.countMax = countMax > 0 ? countMax : 1; 
    }

    
   


    public  int getPercentProcess() {
        if(countMax==0)return 0;
        return Math.min(100, (int) ((long) count * 100 / countMax));
    }

    // Getter và Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() {
        if (this.template != null) {
            return this.template.name.replaceAll("%1", String.valueOf(countMax));
        }
        return "Hiện tại không có nhiệm vụ nào";
    }

   

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getCountMax() { return countMax; }
    public void setCountMax(int countMax) { this.countMax = countMax; }

    
    @Override
    public String toString() {
        return "{" +
                "\"id\":" + id + "," +
             
                "\"count\":" + count + "," +
                "\"countMax\":" + countMax +
                "}";
    }
}
