package com.company.featureflag.evaluation.api.dto;

import java.util.List;

public record BatchEvaluateResponse(List<EvaluateResponse> results) {
}
