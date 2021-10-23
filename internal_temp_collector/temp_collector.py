import requests
import json
import datetime as dt
import logging
import Adafruit_DHT
import time

# run in raspberry pi
# url: https://pimylifeup.com/raspberry-pi-humidity-sensor-dht22/
# installation:
#  sudo apt-get install python3-dev python3-pip
#  sudo python3 -m pip install --upgrade pip setuptools wheel
#  sudo pip3 install Adafruit_DHT

URL = 'http://192.168.2.16:8081/api/temp/internal'
EQUIPMENT_ID = 'pi0'
SLEEP = 600

logging.basicConfig(level=logging.INFO, format='%(asctime)s :: %(levelname)s :: %(message)s')


def post_temp(url: str, equipment_id: str, temperature: float, humidity: int):
  """ Post temperature and humidity values

    url {str} - http server url
    equipment_id {str} - raspberry pi unique id
    temperature {float} - value
    humidity {int} - value
  """
  headers = {'Content-Type': 'application/json'}

  data = {'id': equipment_id, 'temperature': temperature, 'timestamp': round(dt.datetime.now().timestamp()), 'humidity': humidity}

  try:
    resp: requests.Response = requests.post(url, data=json.dumps(data), headers=headers)

    if resp.status_code != 200:
      raise Exception(f"{resp.status_code} - {resp.content}")

    logging.info(f"post response({resp.status_code}): {resp.content}")
  except Exception as ex:
    logging.error(f"post temp fail with error: {ex}")


def main():
  DHT_SENSOR = Adafruit_DHT.DHT22
  DHT_PIN = 4

  while True:
    try:
      # collect data
      humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_PIN)

      # check if data is valid
      if humidity is not None and temperature is not None:
        logging.info(f"id={EQUIPMENT_ID} temperature={round(temperature, 1)}*C  humidity={round(humidity, 1)}%".format(temperature, humidity))
        # post message
        post_temp(URL, EQUIPMENT_ID, temperature, int(round(humidity, 0)))
      else:
        logging.error("failed to retrieve data from humidity sensor")

    except Exception as ex:
      logging.error(f"failed with error: {ex}")
    finally:
      time.sleep(SLEEP)


if __name__ == "__main__":
  main()
