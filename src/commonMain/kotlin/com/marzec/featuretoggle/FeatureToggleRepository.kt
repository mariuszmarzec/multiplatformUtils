package com.marzec.featuretoggle

import com.marzec.dto.FeatureToggleDto
import com.marzec.dto.NewFeatureToggleDto
import com.marzec.dto.UpdateFeatureToggleDto
import com.marzec.model.FeatureToggle
import com.marzec.model.NewFeatureToggle
import com.marzec.model.UpdateFeatureToggle
import com.marzec.repository.CrudRepository

typealias FeatureToggleRepository = CrudRepository<Int, FeatureToggle, NewFeatureToggle, UpdateFeatureToggle, FeatureToggleDto, NewFeatureToggleDto, UpdateFeatureToggleDto>
