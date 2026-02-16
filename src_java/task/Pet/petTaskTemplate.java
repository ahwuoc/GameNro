
package task.Pet;

/**
 *
 * @author ducpro
 */
public class petTaskTemplate {
    public int id;
    public String name;
    public int Maxcount;
    public int Reward;
   
     public petTaskTemplate() {
        this.id = -1;
        this.name = "";
        this.Maxcount = 0;
        this.Reward = 0;
    }
     public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxcount() {
        return Maxcount;
    }

    public void setMaxcount(int Maxcount) {
        this.Maxcount = Maxcount;
    }

    public int getReward() {
        return Reward;
    }

    public void setReward(int Reward) {
        this.Reward = Reward;
    }
}
