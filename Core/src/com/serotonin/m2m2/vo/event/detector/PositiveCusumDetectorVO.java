/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.event.detector;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.rt.event.detectors.PositiveCusumDetectorRT;
import com.serotonin.m2m2.view.text.TextRenderer;

/**
 * @author Terry Packer
 *
 */
public class PositiveCusumDetectorVO extends TimeoutDetectorVO<PositiveCusumDetectorVO>{

	private static final long serialVersionUID = 1L;
	
	@JsonProperty
	private double limit;
	@JsonProperty
	private double weight;

	public PositiveCusumDetectorVO() {
		super(new int[] { DataTypes.NUMERIC });
	}
	
	public double getLimit() {
		return limit;
	}

	public void setLimit(double limit) {
		this.limit = limit;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.TimeoutDetectorVO#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult response) {
		super.validate(response);
		
		if(limit == Double.POSITIVE_INFINITY || limit == Double.NEGATIVE_INFINITY || limit == Double.NaN)
			response.addContextualMessage("limit", "validate.invalidValue");
		if(weight == Double.POSITIVE_INFINITY || weight == Double.NEGATIVE_INFINITY || weight == Double.NaN)
			response.addContextualMessage("weight", "validate.invalidValue");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#createRuntime()
	 */
	@Override
	public AbstractEventDetectorRT<PositiveCusumDetectorVO> createRuntime() {
		return new PositiveCusumDetectorRT(this);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO#getConfigurationDescription()
	 */
	@Override
	protected TranslatableMessage getConfigurationDescription() {
	    if(dataPoint == null)
            dataPoint = DataPointDao.instance.getDataPoint(sourceId);
        TranslatableMessage durationDesc = getDurationDescription();
        
        if (durationDesc == null)
            return new TranslatableMessage("event.detectorVo.posCusum", dataPoint.getTextRenderer().getText(
                    limit, TextRenderer.HINT_SPECIFIC));
        return new TranslatableMessage("event.detectorVo.posCusumPeriod", dataPoint.getTextRenderer()
                    .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
	}

}
