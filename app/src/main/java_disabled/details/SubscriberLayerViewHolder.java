package com.example.ros2_android_test_app.ui.views.details;

import android.view.View;

import com.example.ros2_android_test_app.model.entities.widgets.BaseEntity;
import com.example.ros2_android_test_app.viewmodel.DetailsViewModel;

import java.util.List;

/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 17.03.21
 */
public abstract class SubscriberLayerViewHolder extends DetailViewHolder {

    private LayerViewHolder layerViewHolder;
    private SubscriberViewHolder subscriberViewHolder;


    public SubscriberLayerViewHolder() {
        this.layerViewHolder = new LayerViewHolder(this);
        this.subscriberViewHolder = new SubscriberViewHolder(this);
        this.subscriberViewHolder.topicTypes = this.getTopicTypes();
    }


    public abstract List<String> getTopicTypes();

    @Override
    public void setViewModel(DetailsViewModel viewModel) {
        super.setViewModel(viewModel);
        subscriberViewHolder.viewModel = viewModel;
    }

    public void baseInitView(View view) {
        layerViewHolder.baseInitView(view);
        subscriberViewHolder.baseInitView(view);
    }

    public void baseBindEntity(BaseEntity entity) {
        layerViewHolder.baseBindEntity(entity);
        subscriberViewHolder.baseBindEntity(entity);
    }

    public void baseUpdateEntity(BaseEntity entity) {
        layerViewHolder.baseUpdateEntity(entity);
        subscriberViewHolder.baseUpdateEntity(entity);
    }
}
