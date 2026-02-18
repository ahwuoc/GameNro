package skill;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import models.Template.SkillTemplate;
import java.util.ArrayList;
import java.util.List;
import utils.Util;

public class NClass {

    public int classId;

    public String name;

    public List<SkillTemplate> skillTemplatess = new ArrayList<>();

    public SkillTemplate getSkillTemplate(int tempId) {
        for (SkillTemplate skillTemplate : skillTemplatess) {
            if (skillTemplate.id == tempId) {
                return skillTemplate;
            }
        }
        return null;
    }

    public SkillTemplate getSkillTemplateByName(String name) {
        for (SkillTemplate skillTemplate : skillTemplatess) {
            if ((Util.removeAccent(skillTemplate.name).toUpperCase()).contains((Util.removeAccent(name)).toUpperCase())) {
                return skillTemplate;
            }
        }
        return null;
    }

}
