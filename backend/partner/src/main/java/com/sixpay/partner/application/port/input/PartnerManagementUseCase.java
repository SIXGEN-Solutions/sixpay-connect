package com.sixpay.partner.application.port.input;

import com.sixpay.partner.application.command.ConfigureValidationThresholdCommand;
import com.sixpay.partner.application.command.CreatePartnerCommand;
import com.sixpay.partner.application.command.DecidePartnerCommand;
import com.sixpay.partner.application.command.ReactivatePartnerCommand;
import com.sixpay.partner.application.command.SuspendPartnerCommand;
import com.sixpay.partner.application.view.PartnerView;

public interface PartnerManagementUseCase {

    PartnerView create(CreatePartnerCommand command);

    PartnerView decide(DecidePartnerCommand command);

    PartnerView suspend(SuspendPartnerCommand command);

    PartnerView reactivate(ReactivatePartnerCommand command);

    PartnerView configureValidationThreshold(ConfigureValidationThresholdCommand command);
}
