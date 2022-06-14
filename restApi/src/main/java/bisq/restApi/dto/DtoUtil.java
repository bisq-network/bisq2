package bisq.restApi.dto;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class DtoUtil {

    public static <T> T copyBeanProperties(Object fromObject, T toObject) {
        try {
            Class<? extends Object> fromClass = fromObject.getClass();
            Class<? extends Object> toClass = toObject.getClass();

            BeanInfo fromInfo = Introspector.getBeanInfo(fromClass);
            BeanInfo toInfo = Introspector.getBeanInfo(toClass);
            PropertyDescriptor matchingFromPD; // get some speed in this method

            for (PropertyDescriptor currentToPD : toInfo.getPropertyDescriptors()) {
                // there is always this class object in BeanProperties
                if (currentToPD.getName().equals("class")) {
                    continue;
                }
                // search for corresponding property in from object
                // identified by name and type
                matchingFromPD = null;
                for (PropertyDescriptor currentFromPD : fromInfo.getPropertyDescriptors()) {
                    if (currentFromPD.getName().equals(currentToPD.getName())
                            && currentFromPD.getPropertyType().equals(currentToPD.getPropertyType())) {
                        matchingFromPD = currentFromPD;
                        break;
                    }
                }
                Method writeMethod = currentToPD.getWriteMethod();
                if (matchingFromPD != null && matchingFromPD.getReadMethod() != null && writeMethod != null) {
                    writeMethod.invoke(toObject, matchingFromPD.getReadMethod().invoke(fromObject));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return toObject;
    }

}
