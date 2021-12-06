package com.maiji.magkareble40;
import android.content.res.AssetManager;
import android.util.Log;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelField;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.VoteDistribution;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClfModel {
    private Evaluator evaluator;
    private List<FieldName> activeFields;
    private List<FieldName> targetFields;
    private List<FieldName> outputFields;
    private List<InputField> inputFields;

    public ClfModel() {
        try {
            evaluator = createEvaluator();
            System.out.println("Load model successfully");
            activeFields = getNames(evaluator.getActiveFields());
            targetFields = getNames(evaluator.getTargetFields());
            outputFields = getNames(evaluator.getOutputFields());
            inputFields = evaluator.getInputFields();
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private Evaluator createEvaluator() throws Exception {
        AssetManager assetManager = XBleActivity.getContext().getAssets();

        try(InputStream is = assetManager.open("model.pmml.ser")){
            return MyEvaluatorUtil.createEvaluator(is);
        }
    }

    static
    private List<FieldName> getNames(List<? extends ModelField> modelFields){
        List<FieldName> names = new ArrayList<>(modelFields.size());
//        System.out.println(modelFields.size());

        for(ModelField modelField : modelFields){
            FieldName name = modelField.getName();

            names.add(name);
        }

        return names;
    }

    public int predictMotion(double[] feature) {
        if(feature.length != activeFields.size()){
            Log.i("predictMotion","size is not matched");
        }
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        for(int i=0;i<activeFields.size();i++){
            FieldName inputFieldName = inputFields.get(i).getName();
            FieldValue inputFieldValue = inputFields.get(i).prepare(feature[i]);
            arguments.put(inputFieldName, inputFieldValue);
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        List<TargetField> targetFields = evaluator.getTargetFields();

        FieldName targetFieldName = targetFields.get(0).getName();
        Object targetFieldValue = results.get(targetFieldName);
//        Log.i("targetFieldValue",((VoteDistribution) targetFieldValue).getResult().toString());
        int target= Integer.parseInt(((VoteDistribution) targetFieldValue).getResult().toString());


//        System.out.println(targetFieldValue);

        return target;
    }
}
