package edu.cwru.rise.hslang.structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by {Jian Shi} on 2019/3/28.
 */
public class Dependencies {
    public String left;
    public String right;
    public String dep;

    public Dependencies(String left, String right, String dep) {
        this.left = left;
        this.right = right;
        this.dep = dep;
    }

    /**
     * convert the object to json
     * @return A json string
     */
    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
