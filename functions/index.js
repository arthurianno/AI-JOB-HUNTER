const { onRequest } = require("firebase-functions/v2/https");
const axios = require("axios");

// HeadHunter Client Credentials (configured via environment variables or fallback values)
// To configure them in Firebase Cloud Functions securely, run:
// firebase functions:secrets:set HH_CLIENT_ID=YOUR_CLIENT_ID
// firebase functions:secrets:set HH_CLIENT_SECRET=YOUR_CLIENT_SECRET
const CLIENT_ID = process.env.HH_CLIENT_ID || "YOUR_CLIENT_ID";
const CLIENT_SECRET = process.env.HH_CLIENT_SECRET || "YOUR_CLIENT_SECRET";

exports.exchangeToken = onRequest({ cors: true }, async (req, res) => {
    try {
        const { code, clientId, clientSecret } = req.body;
        
        // Use client credentials from request body or fallback to server environment variables
        const activeClientId = clientId || CLIENT_ID;
        const activeClientSecret = clientSecret || CLIENT_SECRET;

        if (!code) {
            return res.status(400).json({ error: "Missing authorization code" });
        }
        if (activeClientId === "YOUR_CLIENT_ID" || activeClientSecret === "YOUR_CLIENT_SECRET") {
            return res.status(400).json({ 
                error: "HeadHunter OAuth credentials not configured on the server. Please provide them or configure HH_CLIENT_ID and HH_CLIENT_SECRET." 
            });
        }

        const params = new URLSearchParams();
        params.append("grant_type", "authorization_code");
        params.append("client_id", activeClientId);
        params.append("client_secret", activeClientSecret);
        params.append("code", code);
        params.append("redirect_uri", "aijobhunter://oauth");

        const response = await axios.post("https://api.hh.ru/token", params, {
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "User-Agent": "AiJobHunter/1.0 (support@aijobhunter.com)"
            }
        });

        return res.json(response.data);
    } catch (error) {
        console.error("Token exchange failed:", error.response ? error.response.data : error.message);
        const status = error.response ? error.response.status : 500;
        const data = error.response ? error.response.data : { error: error.message };
        return res.status(status).json(data);
    }
});
