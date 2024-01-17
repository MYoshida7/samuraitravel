const stripe = Stripe('pk_test_51OZ5sOF4QzUTAPdWzhSALMUkWq38bi4Lg6B1Ipais9OtucWIyEKRJ6YunFxRCLDhRQ7Ykeodp3zdXJdN9NrW7aaB00v6xHKeKU');
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
  stripe.redirectToCheckout({
    sessionId: sessionId
  })
});