package com.example.samuraitravel.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.samuraitravel.form.ReservationRegisterForm;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class StripeService {
	@Value("${stripe.api-key}")
	private String stripeApiKey;
	
	private final ReservationService reservationService;
	public StripeService(ReservationService reservationService) {
		this.reservationService = reservationService;
	}
	
	// セッションを作成し、Stripeに必要な情報を返す
	public String createStripeSession(
			String houseName,
			ReservationRegisterForm reservationRegisterForm,
			HttpServletRequest httpServletRequest
	) {
		Stripe.apiKey = stripeApiKey;
		String requestUrl = new String(httpServletRequest.getRequestURL());
		SessionCreateParams params = 
			SessionCreateParams.builder()
			.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
			.addLineItem(
				SessionCreateParams.LineItem.builder()
					.setPriceData(
						SessionCreateParams.LineItem.PriceData.builder()
							.setProductData(
								SessionCreateParams.LineItem.PriceData.ProductData.builder()
									.setName(houseName)
									.build()
							)
							.setUnitAmount((long)reservationRegisterForm.getAmount())
							.setCurrency("jpy")
							.build()
					)
					.setQuantity(1L)
					.build()
			)
			.setMode(SessionCreateParams.Mode.PAYMENT)
			.setSuccessUrl(
				requestUrl.replaceAll("/houses/[0-9]+/reservations/confirm", "") + "/reservations?reserved"
			)
			.setCancelUrl(requestUrl.replace("/reservations/confirm", ""))
			.setPaymentIntentData(
				SessionCreateParams.PaymentIntentData.builder()
					.putMetadata("houseId", reservationRegisterForm.getHouseId().toString())
					.putMetadata("userId", reservationRegisterForm.getUserId().toString())
					.putMetadata("checkinDate", reservationRegisterForm.getCheckinDate())
					.putMetadata("checkoutDate", reservationRegisterForm.getCheckoutDate())
					.putMetadata("numberOfPeople", reservationRegisterForm.getNumberOfPeople().toString())
					.putMetadata("amount", reservationRegisterForm.getAmount().toString())
					.build()
			)
			.build();
		try {
			Session session = Session.create(params);
			return session.getId();
		} catch (StripeException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	// セッションから予約情報を取得し、ReservationServiceクラスを介してDBに登録する
	public void processSessionCompleted(Event event) {
		System.out.println(event);
		System.out.println("point2");
		Optional<StripeObject> optionalStripeObject = event.getDataObjectDeserializer().getObject();
		
		System.out.println(optionalStripeObject);

		optionalStripeObject.ifPresent(stripeObject -> {
			System.out.println("point3");
			Session session = (Session)stripeObject;
			SessionRetrieveParams params = SessionRetrieveParams.builder().addExpand("payment_intent").build();
			
			try {
				System.out.println("point4-1");
				session = Session.retrieve(session.getId(), params, null);
				Map<String, String> paymentIntentObject = session.getPaymentIntentObject().getMetadata();
				reservationService.create(paymentIntentObject);
			} catch (StripeException e) {
				System.out.println("point4-2");
				e.printStackTrace();
			}
		});
	}
	/*
	public void processSessionCompleted(Event event) {
	EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
	if (dataObjectDeserializer.getObject().isPresent()) {
	  EventDataObjectDeserializer deserializedDataObject = dataObjectDeserializer.getObject().get();
	  switch (event.getType()) {
	      case "checkout.session.completed":
	          Session session = (Session) deserializedDataObject.getObject();
	          // Sessionオブジェクトを使用して必要な処理を行う
	          break;
	      // 他のイベントタイプに対する処理を追加
	      default:
	          // サポートされていないイベントタイプの場合の処理
	          break;
	  }
	}
	}*/
}
