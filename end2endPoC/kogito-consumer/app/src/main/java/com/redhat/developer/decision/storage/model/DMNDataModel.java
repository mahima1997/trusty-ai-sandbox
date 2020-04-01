package com.redhat.developer.decision.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DMNDataModel {

    @JsonProperty("result")
    public DMNResultModel result;

    public DMNDataModel(){}

    public DMNDataModel(DMNResultModel resultModel){
        this.result = resultModel;
    }
}
