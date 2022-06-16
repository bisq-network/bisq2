package bisq.restApi.dto;

import lombok.Data;

@Data
public class BaseDto<T extends BaseDto<?>> {

    public T loadFieldsFrom(Object domainObject) {
        DtoUtil.copyBeanProperties(domainObject, this);
        return (T) this;
    }

}
